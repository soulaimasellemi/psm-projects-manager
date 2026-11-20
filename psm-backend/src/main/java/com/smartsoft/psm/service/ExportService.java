package com.smartsoft.psm.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smartsoft.psm.entity.Client;
import com.smartsoft.psm.entity.Projet;
import com.smartsoft.psm.entity.StatutProjet;
import com.smartsoft.psm.entity.User;
import com.smartsoft.psm.exception.ApiException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Export des listes clients / projets en Excel (.xlsx, Apache POI) ou PDF (OpenPDF).
 * Les deux formats partagent la même définition de colonnes.
 */
@Service
public class ExportService {

    public enum Format {
        XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        PDF("pdf", "application/pdf");

        public final String extension;
        public final String mimeType;

        Format(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }

        public static Format parse(String value) {
            try {
                return Format.valueOf(value.trim().toUpperCase());
            } catch (RuntimeException e) {
                throw ApiException.badRequest("Format d'export inconnu : " + value + " (attendu : xlsx ou pdf)");
            }
        }
    }

    /** Fichier généré, prêt à être renvoyé au navigateur */
    public record Fichier(byte[] contenu, String nom, String mimeType) {}

    /** Colonne : titre, valeur (String ou Number), largeur relative */
    private record Colonne<T>(String titre, Function<T, Object> valeur, float largeur) {}

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color PRIMAIRE = new Color(94, 114, 228);

    // ---------- Définition des colonnes ----------

    private static final List<Colonne<Client>> COLONNES_CLIENTS = List.of(
            new Colonne<>("Nom", Client::getNom, 3f),
            new Colonne<>("Secteur", c -> ouVide(c.getSecteur()), 2.2f),
            new Colonne<>("Email", c -> ouVide(c.getEmail()), 3f),
            new Colonne<>("Téléphone", c -> ouVide(c.getTel()), 2f),
            new Colonne<>("Adresse", c -> ouVide(c.getAdresse()), 3f),
            new Colonne<>("Projets", c -> c.getProjets().size(), 1f),
            new Colonne<>("Budget total (TND)", c -> c.getProjets().stream().mapToDouble(Projet::getBudget).sum(), 1.8f));

    private static final List<Colonne<Projet>> COLONNES_PROJETS = List.of(
            new Colonne<>("Projet", Projet::getLibelle, 3f),
            new Colonne<>("Client", p -> p.getClient().getNom(), 2.2f),
            new Colonne<>("Statut", p -> statut(p.getStatut()) + (p.isEnRetard() ? " (en retard)" : ""), 1.8f),
            new Colonne<>("Avancement (%)", Projet::getAvancement, 1.3f),
            new Colonne<>("Budget (TND)", Projet::getBudget, 1.5f),
            new Colonne<>("Durée (mois)", Projet::getDuree, 1.1f),
            new Colonne<>("Début", p -> date(p.getDateDebut()), 1.4f),
            new Colonne<>("Fin prévue", p -> date(p.getDateFinPrevue()), 1.4f),
            new Colonne<>("Équipe", p -> p.getMembres().stream()
                    .sorted(Comparator.comparing(User::getPrenom))
                    .map(User::getNomComplet).collect(Collectors.joining(", ")), 3f));

    // ---------- API publique ----------

    public Fichier exporterClients(List<Client> clients, Format format) {
        return exporter("Liste des clients", "clients", COLONNES_CLIENTS, clients, format);
    }

    public Fichier exporterProjets(List<Projet> projets, Format format) {
        return exporter("Liste des projets", "projets", COLONNES_PROJETS, projets, format);
    }

    private <T> Fichier exporter(String titre, String prefixe, List<Colonne<T>> colonnes, List<T> lignes, Format format) {
        byte[] contenu = format == Format.XLSX ? excel(titre, colonnes, lignes) : pdf(titre, colonnes, lignes);
        String nom = "psm-" + prefixe + "-" + LocalDate.now() + "." + format.extension;
        return new Fichier(contenu, nom, format.mimeType);
    }

    // ---------- Excel ----------

    private <T> byte[] excel(String titre, List<Colonne<T>> colonnes, List<T> lignes) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(titre);

            org.apache.poi.ss.usermodel.Font gras = wb.createFont();
            gras.setBold(true);
            gras.setColor(IndexedColors.WHITE.getIndex());
            CellStyle entete = wb.createCellStyle();
            entete.setFont(gras);
            entete.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            entete.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            entete.setAlignment(HorizontalAlignment.CENTER);

            CellStyle nombre = wb.createCellStyle();
            nombre.setDataFormat(wb.createDataFormat().getFormat("#,##0"));

            Row header = sheet.createRow(0);
            for (int i = 0; i < colonnes.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(colonnes.get(i).titre());
                cell.setCellStyle(entete);
            }

            int r = 1;
            for (T ligne : lignes) {
                Row row = sheet.createRow(r++);
                for (int i = 0; i < colonnes.size(); i++) {
                    Object v = colonnes.get(i).valeur().apply(ligne);
                    Cell cell = row.createCell(i);
                    if (v instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                        cell.setCellStyle(nombre);
                    } else {
                        cell.setCellValue(v == null ? "" : v.toString());
                    }
                }
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(r - 1, 0), 0, colonnes.size() - 1));
            for (int i = 0; i < colonnes.size(); i++) {
                sheet.setColumnWidth(i, Math.min(60, Math.round(colonnes.get(i).largeur() * 7)) * 256);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la génération du fichier Excel", e);
        }
    }

    // ---------- PDF ----------

    private <T> byte[] pdf(String titre, List<Colonne<T>> colonnes, List<T> lignes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font fTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PRIMAIRE);
        Font fSous = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
        Font fEntete = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font fCellule = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, new Color(50, 50, 93));

        doc.add(new Paragraph("SmartSoft Solutions — " + titre, fTitre));
        doc.add(new Paragraph("Projects Manager · exporté le " + LocalDateTime.now().format(HORODATAGE)
                + " · " + lignes.size() + " ligne(s)", fSous));
        doc.add(new Paragraph(" "));

        float[] largeurs = new float[colonnes.size()];
        for (int i = 0; i < largeurs.length; i++) largeurs[i] = colonnes.get(i).largeur();
        PdfPTable table = new PdfPTable(largeurs);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        for (Colonne<T> c : colonnes) {
            PdfPCell cell = new PdfPCell(new Phrase(c.titre(), fEntete));
            cell.setBackgroundColor(PRIMAIRE);
            cell.setPadding(5);
            cell.setBorderColor(Color.WHITE);
            table.addCell(cell);
        }

        List<Color> fonds = new ArrayList<>(List.of(Color.WHITE, new Color(246, 249, 252)));
        int n = 0;
        for (T ligne : lignes) {
            Color fond = fonds.get(n++ % 2);
            for (Colonne<T> c : colonnes) {
                Object v = c.valeur().apply(ligne);
                String texte = v instanceof Number num ? String.format("%,.0f", num.doubleValue()).replace(' ', ' ') : String.valueOf(v);
                PdfPCell cell = new PdfPCell(new Phrase(texte, fCellule));
                cell.setBackgroundColor(fond);
                cell.setPadding(4);
                cell.setBorderColor(new Color(233, 236, 239));
                if (v instanceof Number) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cell);
            }
        }
        if (lignes.isEmpty()) {
            PdfPCell vide = new PdfPCell(new Phrase("Aucune donnée", fCellule));
            vide.setColspan(colonnes.size());
            vide.setPadding(8);
            vide.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(vide);
        }

        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    // ---------- Utilitaires ----------

    private static String ouVide(String s) {
        return s == null ? "" : s;
    }

    private static String date(LocalDate d) {
        return d == null ? "" : d.format(DATE);
    }

    private static String statut(StatutProjet s) {
        return switch (s) {
            case EN_ATTENTE -> "En attente";
            case EN_COURS -> "En cours";
            case TERMINE -> "Terminé";
        };
    }
}
