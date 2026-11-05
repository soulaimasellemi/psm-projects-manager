@echo off
REM =============================================================
REM  Construit psm.jar : Angular (build prod) + Spring Boot
REM  A lancer depuis le dossier racine du projet (double-clic)
REM =============================================================
echo [1/3] Build Angular (production)...
cd psm-frontend
call npm install || goto :error
call npx ng build --configuration production || goto :error
cd ..

echo [2/3] Copie de dist vers psm-backend\src\main\resources\static ...
if exist psm-backend\src\main\resources\static rmdir /s /q psm-backend\src\main\resources\static
xcopy /E /I /Y psm-frontend\dist\psm-frontend\browser psm-backend\src\main\resources\static >nul || goto :error

echo [3/3] Build Spring Boot (mvn package)...
cd psm-backend
call mvn clean package -DskipTests || goto :error
cd ..

echo.
echo ===== OK : psm-backend\target\psm.jar est pret =====
echo Lancer :  java -jar psm-backend\target\psm.jar
echo Puis ouvrir http://localhost:8080
pause
exit /b 0

:error
echo.
echo ===== ERREUR pendant le build (voir les messages ci-dessus) =====
pause
exit /b 1
