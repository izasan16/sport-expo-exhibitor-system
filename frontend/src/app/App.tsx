import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import MobileUnsupported from './MobileUnsupported';
import { routes } from './routes';

const router = createBrowserRouter(routes);

export default function App() {
  return (
    <MobileUnsupported>
      <RouterProvider router={router} />
    </MobileUnsupported>
  );
}
