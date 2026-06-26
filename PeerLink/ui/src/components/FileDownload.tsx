'use client';

import { useState } from 'react';
import { FiDownload } from 'react-icons/fi';

interface FileDownloadProps {
  onDownload: (port: number) => Promise<void>;
  isDownloading: boolean;
}

export default function FileDownload({ onDownload, isDownloading }: FileDownloadProps) {
  const [inviteCode, setInviteCode] = useState('');
  const [error, setError] = useState('');
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    const port = parseInt(inviteCode.trim(), 10);
    if (isNaN(port) || port <= 0 || port > 65535) {
      setError('Please enter a valid port number (1-65535)');
      return;
    }
    
    try {
      await onDownload(port);
    } catch (err) {
      setError('Failed to download the file. Please check the invite code and try again.');
    }
  };
  
  return (
    <div className="w-full">
      <div className="bg-[#1a1a1a] p-6 border border-[#3c3c3c] mb-6">
        <h3 className="title-lg text-white mb-2 uppercase">Receive a File</h3>
        <p className="body-md text-[#bbbbbb] mb-0">
          Enter the invite code shared with you to download the file.
        </p>
      </div>
      
      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label htmlFor="inviteCode" className="block label-uppercase text-[#bbbbbb] mb-2">
            INVITE CODE
          </label>
          <input
            type="text"
            id="inviteCode"
            value={inviteCode}
            onChange={(e) => setInviteCode(e.target.value)}
            placeholder="ENTER PORT NUMBER"
            className="input-field uppercase placeholder:text-[#7e7e7e]"
            disabled={isDownloading}
            required
          />
          {error && <p className="mt-2 body-sm text-[#e22718]">{error}</p>}
        </div>
        
        <button
          type="submit"
          className="btn-primary w-full"
          disabled={isDownloading}
        >
          {isDownloading ? (
            <span>DOWNLOADING...</span>
          ) : (
            <>
              <FiDownload className="w-4 h-4 mr-3" />
              <span>DOWNLOAD FILE</span>
            </>
          )}
        </button>
      </form>
    </div>
  );
}
