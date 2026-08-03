import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { AuthProvider } from './AuthProvider';
import { routes } from './routes';

const router = createBrowserRouter(routes);

export default function App() {
  return (
    <AuthProvider>
      <RouterProvider router={router} future={{ v7_startTransition: true }} />
    </AuthProvider>
  );
}
