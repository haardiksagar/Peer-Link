'use client';

import { useState } from 'react';
import { FiCopy, FiCheck } from 'react-icons/fi';

interface InviteCodeProps {
  port: number | null;
}

export default function InviteCode({ port }: InviteCodeProps) {
  const [copied, setCopied] = useState(false);

  if (!port) return null;

  const copyToClipboard = () => {
    navigator.clipboard.writeText(port.toString());
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="mt-4 p-6 bg-[#1a1a1a]/40 backdrop-blur-md border border-[#3c3c3c] rounded-2xl">
      <h3 className="title-lg text-white mb-2 uppercase">File Ready to Share</h3>
      <p className="body-md text-[#bbbbbb] mb-6">
        Share this invite code with anyone you want to share the file with:
      </p>

      <div className="flex items-center h-[48px]">
        <div className="flex-1 h-full bg-[#0d0d0d] px-4 flex items-center border border-r-0 border-[#3c3c3c] font-mono text-[20px] text-white">
          {port}
        </div>
        <button
          onClick={copyToClipboard}
          className="h-full px-6 bg-transparent text-white border border-[#3c3c3c] hover:bg-white hover:text-black hover:border-white transition-colors flex items-center justify-center"
          aria-label="Copy invite code"
        >
          {copied ? <FiCheck className="w-5 h-5" /> : <FiCopy className="w-5 h-5" />}
        </button>
      </div>

      <p className="mt-4 body-sm text-[#7e7e7e]">
        This code will be valid as long as your file sharing session is active.
      </p>
    </div>
  );
}
