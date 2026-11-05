import { Directive, TemplateRef, ViewContainerRef, effect, inject, input } from '@angular/core';
import { AuthService } from '../core/services/auth.service';
import { RoleName } from '../core/models';

/**
 * Affiche un élément seulement si l'utilisateur a le rôle demandé.
 * Exemple : <button *appHasRole="'ADMIN'">Supprimer</button>
 */
@Directive({ selector: '[appHasRole]' })
export class HasRole {
  private tpl = inject<TemplateRef<unknown>>(TemplateRef);
  private vcr = inject(ViewContainerRef);
  private auth = inject(AuthService);

  readonly appHasRole = input.required<RoleName | RoleName[]>();
  private shown = false;

  constructor() {
    effect(() => {
      const value = this.appHasRole();
      const roles = Array.isArray(value) ? value : [value];
      const allowed = this.auth.hasRole(...roles);
      if (allowed && !this.shown) {
        this.vcr.createEmbeddedView(this.tpl);
        this.shown = true;
      } else if (!allowed && this.shown) {
        this.vcr.clear();
        this.shown = false;
      }
    });
  }
}
