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

export type ParticipationOrder = {
  id: string;
  eventId: string;
  exhibitorId: string;
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

export type PaymentRecord = {
  id: string;
  eventId: string;
  orderId: string;
  amount: number | string;
  status: PaymentStatus;
  method: string;
  confirmedAt?: string | null;
  createdAt: string;
  updatedAt: string;
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
  boothIds: string[];
  createdAt: string;
  updatedAt: string;
};

export type Booth = {
  id: string;
  eventId: string;
  boothNo: string;
  areaSqm: number | string;
  boothType: string;
  status: BoothStatus;
};
