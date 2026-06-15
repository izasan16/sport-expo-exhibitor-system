import { formatRegistrationStatus } from '../../app/display';
import type { RegistrationStatus } from '../../app/domain';

export { formatRegistrationStatus };

type RegistrationActionSource = {
  id: string;
  status: RegistrationStatus;
};

export type RegistrationHomeAction = {
  label: string;
  to: string;
  disabled: boolean;
  reason: string;
};

export type RegistrationListPrimaryAction = {
  label: string;
  to: string;
};

export function getRegistrationHomeAction(registration?: RegistrationActionSource): RegistrationHomeAction {
  if (!registration) {
    return {
      label: '提交报名',
      to: '/exhibitor/registrations/new',
      disabled: false,
      reason: '',
    };
  }

  if (registration.status === 'APPROVED') {
    return {
      label: '去下单',
      to: '/exhibitor/orders/new',
      disabled: false,
      reason: '',
    };
  }

  if (registration.status === 'REJECTED') {
    return {
      label: '修改并重新提交',
      to: `/exhibitor/registrations/${registration.id}`,
      disabled: false,
      reason: '',
    };
  }

  if (registration.status === 'CANCELLED') {
    return {
      label: '重新报名',
      to: '/exhibitor/registrations/new',
      disabled: false,
      reason: '',
    };
  }

  return {
    label: '查看报名进度',
    to: `/exhibitor/registrations/${registration.id}`,
    disabled: false,
    reason: '',
  };
}

export function getLatestRegistration<T extends { createdAt?: string | null; updatedAt?: string | null }>(registrations: T[]) {
  return [...registrations].sort((left, right) => {
    const leftTime = Date.parse(left.updatedAt ?? left.createdAt ?? '') || 0;
    const rightTime = Date.parse(right.updatedAt ?? right.createdAt ?? '') || 0;
    return rightTime - leftTime;
  })[0];
}

export function getPrimaryRegistrationForHome<T extends RegistrationActionSource & { createdAt?: string | null; updatedAt?: string | null }>(
  registrations: T[],
) {
  return registrations.find((registration) => registration.status === 'APPROVED') ?? getLatestRegistration(registrations);
}

export function getRegistrationListPrimaryAction<T extends RegistrationActionSource & { createdAt?: string | null; updatedAt?: string | null }>(
  registrations: T[],
): RegistrationListPrimaryAction {
  const latestRejected = getLatestRegistration(registrations.filter((registration) => registration.status === 'REJECTED'));
  if (latestRejected) {
    return {
      label: '修改并重新提交',
      to: `/exhibitor/registrations/${latestRejected.id}`,
    };
  }

  return {
    label: '提交报名',
    to: '/exhibitor/registrations/new',
  };
}
