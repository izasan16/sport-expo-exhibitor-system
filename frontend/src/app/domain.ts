export type OrderStatus =
  | 'PENDING_SALES_CONFIRM'
  | 'PENDING_CONTRACT'
  | 'PENDING_PAYMENT'
  | 'PARTIALLY_PAID'
  | 'PAID'
  | 'COMPLETED';

export type PaymentStatus = 'PENDING_FINANCE_CONFIRM' | 'CONFIRMED' | 'CONFIRM_FAILED';
export type ContractStatus = 'GENERATED' | 'EFFECTIVE';
export type ContractSignStatus = 'UNSIGNED' | 'MARKED_SIGNED';
export type SelectionStatus = 'PENDING_REVIEW' | 'CONFIRMED' | 'REJECTED' | 'TIMEOUT';
export type BoothStatus = 'AVAILABLE' | 'LOCKED' | 'CONFIRMED' | 'RESERVED' | 'DISABLED';
export type EligibilityStatus = 'NOT_OPEN' | 'AVAILABLE' | 'PAUSED' | 'CLOSED';
export type ExhibitorStatus = 'ACTIVE' | 'SUSPENDED' | 'ARCHIVED';
export type RegistrationStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export type Exhibitor = {
  id: string;
  companyName: string;
  creditCode?: string | null;
  contactName?: string | null;
  contactPhone?: string | null;
  contactEmail?: string | null;
  address?: string | null;
  status: ExhibitorStatus;
  createdAt: string;
  updatedAt: string;
};

export type EventCategory = {
  id: string;
  eventId: string;
  code: string;
  name: string;
  enabled: boolean;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
};

export type Registration = {
  id: string;
  eventId: string;
  exhibitorId: string;
  status: RegistrationStatus;
  category?: string | null;
  categoryName?: string | null;
  zoneId?: string | null;
  intentAreaSqm?: string | number | null;
  contactName?: string | null;
  contactPhone?: string | null;
  remark?: string | null;
  reviewComment?: string | null;
  reviewedBy?: string | null;
  reviewedAt?: string | null;
  submittedAt?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ParticipationOrder = {
  id: string;
  eventId: string;
  exhibitorId: string;
  registrationId?: string | null;
  zoneId?: string | null;
  areaTierId: string;
  areaSqm: number | string;
  boothType: string;
  status: OrderStatus;
  areaFee: number | string;
  serviceFee: number | string;
  discountAmount: number | string;
  adjustmentAmount: number | string;
  totalAmount: number | string;
  createdAt: string;
  updatedAt: string;
};

export type BatchConfirmOrdersResponse = {
  successCount: number;
  failureCount: number;
  failures: Array<{
    orderId: string;
    reason: string;
  }>;
};

export type PaymentRecord = {
  id: string;
  eventId?: string | null;
  orderId: string;
  amount?: number | string | null;
  status: PaymentStatus;
  method?: string | null;
  failureReason?: string | null;
  confirmedAt?: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy?: string | null;
  updatedBy?: string | null;
};

export type ContractRecord = {
  id: string;
  eventId: string;
  orderId: string;
  currentVersionId?: string | null;
  contractNo: string;
  status: ContractStatus;
  createdAt: string;
  updatedAt: string;
};

export type ContractVersionRecord = {
  id: string;
  eventId: string;
  contractId: string;
  versionNo: number;
  reason: string;
  amount: number | string;
  areaSqm: number | string;
  signStatus: ContractSignStatus;
  signerId?: string | null;
  signedAt?: string | null;
  fileId?: string | null;
  createdAt: string;
  createdBy?: string | null;
};

export type SelectionEligibility = {
  id: string;
  eventId: string;
  orderId: string;
  status: EligibilityStatus;
  pauseReason?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type BoothSelection = {
  id: string;
  eventId: string;
  orderId: string;
  exhibitorId: string;
  status: SelectionStatus;
  totalAreaSqm: number | string;
  lockStartedAt?: string | null;
  lockExpiresAt?: string | null;
  helperUserId?: string | null;
  reviewerUserId?: string | null;
  reviewedAt?: string | null;
  reviewComment?: string | null;
  exhibitorRemark?: string | null;
  recommendedBooths?: string | null;
  boothIds: string[];
  createdAt: string;
  updatedAt: string;
  createdBy?: string | null;
  updatedBy?: string | null;
};

export type Booth = {
  id: string;
  eventId: string;
  boothNo: string;
  areaSqm: number | string;
  boothType: string;
  status: BoothStatus;
};

export type AdminDashboardResponse = {
  generatedAt?: string | null;
  pendingSalesConfirmCount?: number | null;
  pendingFinanceConfirmCount?: number | null;
  pendingSelectionReviewCount?: number | null;
  todayNewOrderCount?: number | null;
  pendingSalesConfirmOrders?: ParticipationOrder[];
  pendingFinanceConfirmPayments?: PaymentRecord[];
  pendingSelectionReviews?: BoothSelection[];
};
