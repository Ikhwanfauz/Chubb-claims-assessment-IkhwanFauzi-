import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export const statuses = ['SUBMITTED', 'UNDER_REVIEW', 'AWAITING_INFORMATION', 'SETTLED', 'REJECTED'] as const;
type Status = typeof statuses[number];
export type Outcome = 'SETTLED' | 'REJECTED';

export interface Submission {
  type: 'MOTOR' | 'PROPERTY';
  market: string;
  incidentDate: string;
  description: string;
}

export interface Claim extends Submission {
  id: string;
  status: Status;
  assignedOfficer: string | null;
  decisionReason: string | null;
  informationRequest: { id: string; question: string; answer: string | null } | null;
  estimatedAmount: number | null;
  estimateCurrency: string | null;
}

export interface Workload {
  totalClaims: number;
  openClaims: number;
  byStatus: Record<Status, number>;
  byOfficer: { officer: string; openClaims: number; byStatus: Record<Status, number> }[];
}

export interface Exposure {
  openClaimsWithoutEstimate: number;
  byCurrency: Record<string, { estimatedAmount: number; claimCount: number }>;
}

@Injectable({ providedIn: 'root' })
export class ClaimsApi {
  private readonly http = inject(HttpClient);

  submit(input: Submission) {
    return firstValueFrom(this.http.post<Claim>('/api/claims', input));
  }

  find(id: string) {
    return firstValueFrom(this.http.get<Claim>(this.claimUrl(id)));
  }

  list() {
    return firstValueFrom(this.http.get<Claim[]>('/api/claims'));
  }

  workload() {
    return firstValueFrom(this.http.get<Workload>('/api/staff/workload'));
  }

  exposure() {
    return firstValueFrom(this.http.get<Exposure>('/api/staff/exposure'));
  }

  assign(id: string, officer: string) {
    return this.post(id, 'assignment', { officer });
  }

  ask(id: string, question: string) {
    return this.post(id, 'information-requests', { question });
  }

  reply(id: string, requestId: string, answer: string) {
    return this.post(id, 'information-requests/' + encodeURIComponent(requestId) + '/response', { answer });
  }

  decide(id: string, outcome: Outcome, reason: string) {
    return this.post(id, 'decision', { outcome, reason });
  }

  estimate(id: string, amount: number, currency: string) {
    return firstValueFrom(this.http.put<Claim>(this.claimUrl(id) + '/estimate', { amount, currency }));
  }

  private post(id: string, route: string, data: object) {
    return firstValueFrom(this.http.post<Claim>(this.claimUrl(id) + '/' + route, data));
  }

  private claimUrl(id: string) {
    return '/api/claims/' + encodeURIComponent(id);
  }
}
