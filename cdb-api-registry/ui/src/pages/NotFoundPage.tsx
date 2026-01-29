import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="container-responsive py-20 text-center">
      <h1 className="text-4xl font-bold text-gray-900">404</h1>
      <p className="mt-2 text-gray-600">The page you’re looking for doesn’t exist.</p>
      <Link className="mt-6 inline-block rounded-md bg-brand px-4 py-2 text-white" to="/">Go back home</Link>
    </div>
  )
}
