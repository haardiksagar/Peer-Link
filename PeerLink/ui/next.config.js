/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  swcMinify: true,
  async rewrites() {
    const backendUrl = process.env.BACKEND_URL || 'http://localhost:8080';
    return [
      {
        source: '/api/upload',
        destination: `${backendUrl}/upload`,
      },
      {
        source: '/api/download/:port',
        destination: `${backendUrl}/download/:port`,
      },
    ];
  },
}

module.exports = nextConfig
