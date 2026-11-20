import { CurrencyPipe, KeyValuePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Claim, ClaimsApi, Exposure, Outcome, statuses, Submission, Workload } from './claims-api';

@Component({
  selector: 'claims-app',
  imports: [FormsModule, CurrencyPipe, KeyValuePipe],
  templateUrl: './app.html',
})
export class App {
  private readonly api = inject(ClaimsApi);

  readonly view = signal<'claimant' | 'staff'>('claimant');
  readonly busy = signal(false);
  readonly error = signal('');
  readonly statusOptions = statuses;
  readonly claimantClaim = signal<Claim | null>(null);
  readonly selected = signal<Claim | null>(null);
  readonly claims = signal<Claim[]>([]);
  readonly workload = signal<Workload | null>(null);
  readonly exposure = signal<Exposure | null>(null);

  submission: Submission = { type: 'MOTOR', market: 'MY', incidentDate: '', description: '' };
  lookup = '';
  answer = '';
  officer = 'Officer A';
  question = '';
  amount: number | null = null;
  currency = 'MYR';
  outcome: Outcome = 'SETTLED';
  reason = '';

  switchView(view: 'claimant' | 'staff') {
    this.view.set(view);
    this.error.set('');
    if (view === 'staff') {
      this.refresh();
    }
  }

  submit() {
    return this.run(async () => {
      const claim = await this.api.submit({ ...this.submission, market: this.submission.market.trim().toUpperCase() });
      this.lookup = claim.id;
      this.answer = '';
      this.claimantClaim.set(claim);
    });
  }

  track() {
    return this.run(async () => {
      this.claimantClaim.set(null);
      this.answer = '';
      this.claimantClaim.set(await this.api.find(this.lookup.trim()));
    });
  }

  reply(claim: Claim) {
    const request = claim.informationRequest;
    if (request) {
      return this.run(async () => {
        this.claimantClaim.set(await this.api.reply(claim.id, request.id, this.answer));
        this.answer = '';
      });
    }
    return Promise.resolve();
  }

  refresh() {
    return this.run(() => this.refreshStaff());
  }

  select(claim: Claim) {
    this.selected.set(claim);
    this.question = '';
    this.reason = '';
    this.amount = claim.estimatedAmount;
    this.currency = claim.estimateCurrency ?? 'MYR';
  }

  assign(claim: Claim) {
    return this.updateStaff(() => this.api.assign(claim.id, this.officer));
  }

  ask(claim: Claim) {
    return this.updateStaff(() => this.api.ask(claim.id, this.question));
  }

  saveEstimate(claim: Claim) {
    const amount = this.amount;
    if (amount !== null) {
      return this.updateStaff(() => this.api.estimate(claim.id, amount, this.currency.trim().toUpperCase()));
    }
    return Promise.resolve();
  }

  decide(claim: Claim) {
    return this.updateStaff(() => this.api.decide(claim.id, this.outcome, this.reason));
  }

  private updateStaff(update: () => Promise<Claim>) {
    return this.run(async () => {
      this.select(await update());
      await this.refreshStaff();
    });
  }

  private async refreshStaff() {
    const [claims, workload, exposure] = await Promise.all([this.api.list(), this.api.workload(), this.api.exposure()]);
    this.claims.set(claims);
    this.workload.set(workload);
    this.exposure.set(exposure);

    const selected = this.selected();
    if (selected) {
      const current = claims.find(claim => claim.id === selected.id);
      if (current && JSON.stringify(current) !== JSON.stringify(selected)) {
        this.select(current);
      }
    }
  }

  private async run(action: () => Promise<void>) {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set('');
    try {
      await action();
    } catch (error: unknown) {
      if (error instanceof HttpErrorResponse) {
        this.error.set(error.status === 0 || error.status >= 500
          ? 'The API is unavailable. Check that the backend is running.'
          : error.error?.detail ?? 'The API could not complete this request.');
      } else {
        this.error.set('Something went wrong. Try again.');
      }
    } finally {
      this.busy.set(false);
    }
  }
}
