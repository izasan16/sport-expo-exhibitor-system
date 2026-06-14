import { Navigate, useSearchParams } from 'react-router-dom';
import { buildLegacyOrderTarget, type LegacyOrderTarget } from '../pages/exhibitor/pageLogic';

export function RedirectWithOrderId({ target }: { target: LegacyOrderTarget }) {
  const [searchParams] = useSearchParams();
  const orderId = searchParams.get('orderId');

  if (!orderId) {
    return <Navigate to="/exhibitor/orders" replace state={{ message: '请先选择订单' }} />;
  }

  return <Navigate to={buildLegacyOrderTarget(target, orderId)} replace />;
}
