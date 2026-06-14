import { Navigate, useSearchParams } from 'react-router-dom';

export function RedirectWithOrderId({ target }: { target: 'payment' | 'selection' | 'detail' }) {
  const [searchParams] = useSearchParams();
  const orderId = searchParams.get('orderId');

  if (!orderId) {
    return <Navigate to="/exhibitor/orders" replace state={{ message: '请先选择订单' }} />;
  }

  if (target === 'payment') {
    return <Navigate to={`/exhibitor/orders/${orderId}/payment`} replace />;
  }
  if (target === 'selection') {
    return <Navigate to={`/exhibitor/orders/${orderId}/selection`} replace />;
  }
  return <Navigate to={`/exhibitor/orders/${orderId}`} replace />;
}
