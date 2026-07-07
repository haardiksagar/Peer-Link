import './globals.css'
import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import BackgroundGradient from '@/components/BackgroundGradient'

const inter = Inter({ subsets: ['latin'], weight: ['300', '700'] })

export const metadata: Metadata = {
  title: 'PeerLink - P2P File Sharing',
  description: 'Securely share files peer-to-peer',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <BackgroundGradient />
        <header className="w-full bg-[#000]/80 backdrop-blur-sm h-[64px] flex items-center px-8 border-b border-[#3c3c3c] relative z-10">
          <div className="flex items-center space-x-2">
            <span className="display-sm text-white m-0 leading-none">PEERLINK</span>
          </div>
        </header>
        <div className="m-stripe-divider relative z-10">
          <div className="m-stripe-segment-1"></div>
          <div className="m-stripe-segment-2"></div>
          <div className="m-stripe-segment-3"></div>
        </div>
        <main className="min-h-screen relative z-10">
          {children}
        </main>
      </body>
    </html>
  )
}
