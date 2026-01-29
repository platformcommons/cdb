import Header from './Header'
import Sidebar from './Sidebar'
import { ReactNode, useState } from 'react'

export default function Layout({ children }: { children: ReactNode }) {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true)

  return (
    <div className="min-h-full flex flex-col">
      <Header onToggleSidebar={() => setIsSidebarOpen(v => !v)} isSidebarOpen={isSidebarOpen} />
      <div className="flex flex-1">
        <Sidebar open={isSidebarOpen} />
        <main className="flex-1">
          <div className={`py-4 ${isSidebarOpen ? 'container-responsive' : 'px-4'}`}>
            {children}
          </div>
        </main>
      </div>
      <footer className="border-t bg-white">
        <div className="container-responsive text-sm text-gray-500 py-4">
          © {new Date().getFullYear()} Platform Commons · API Registry
        </div>
      </footer>
    </div>
  )
}
