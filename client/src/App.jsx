import React, { useState, useRef } from 'react';

function App() {
  const [tracks, setTracks] = useState([]);
  const [selectedTrack, setSelectedTrack] = useState(null);
  const [uploading, setUploading] = useState(false);
  const audioRef = useRef();

  // 曲アップロード
  const handleUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setUploading(true);
    // APIにアップロード
    const formData = new FormData();
    formData.append('file', file);
    const res = await fetch('/api/tracks', {
      method: 'POST',
      body: formData,
    });
    if (res.ok) {
      // アップロード後に一覧を再取得
      await fetchTracks();
    }
    setUploading(false);
  };

  // 曲一覧取得
  const fetchTracks = async () => {
    const res = await fetch('/api/tracks');
    if (res.ok) {
      const data = await res.json();
      // filePathをurlに変換
      const tracksWithUrl = data.map(track => ({
        ...track,
        url: track.filePath.startsWith('/') ? track.filePath : `/${track.filePath}`
      }));
      setTracks(tracksWithUrl);
    }
  }

  React.useEffect(() => {
    fetchTracks();
  }, []);

  // 曲再生
  const handlePlay = (track) => {
    setSelectedTrack(track);
  }

  // selectedTrack変更時にaudio再生
  React.useEffect(() => {
    if (selectedTrack && audioRef.current) {
      audioRef.current.play().catch(() => {});
    }
  }, [selectedTrack]);

  // 曲削除
  const handleDelete = async (track) => {
    if (!window.confirm('この曲を削除してもよろしいですか？')) {
      return;
    }
    
    const res = await fetch(`/api/tracks/${track.id}`, {
      method: 'DELETE'
    });
    
    if (res.ok) {
      // 削除成功したら一覧を再取得
      await fetchTracks();
    }
  };

  return (
    <div style={{ maxWidth: 480, margin: '0 auto', padding: 16 }}>
      <h1>曲アップロード＆共有アプリ</h1>
      <div style={{ marginBottom: 24 }}>
        <input type="file" accept="audio/*" onChange={handleUpload} disabled={uploading} />
        {uploading && <span>アップロード中...</span>}
      </div>
      <h2>曲一覧</h2>
      <ul>
        {tracks.map(track => (
          <li key={track.id} style={{ marginBottom: 8 }}>
            <button onClick={() => handlePlay(track)} style={{ marginRight: 8 }}>再生</button>
            {track.title || track.name}
            <button 
              onClick={() => handleDelete(track)} 
              style={{ marginLeft: 8, backgroundColor: '#ff4444', color: 'white' }}
            >
              削除
            </button>
            <span style={{ marginLeft: 8, color: '#888', fontSize: 12 }}>
              {track.createdAt ? (() => {
                // ISO8601文字列をDateに変換（Safari対応）
                const dateStr = track.createdAt.replace('T', ' ').replace(/-/g, '/');
                return new Date(dateStr).toLocaleString();
              })() : ''}
            </span>
          </li>
        ))}
      </ul>
      {selectedTrack && (
        <div style={{ marginTop: 32 }}>
          <h3>再生中: {selectedTrack.title || selectedTrack.name}</h3>
          <audio
            key={selectedTrack?.id}
            ref={audioRef}
            src={selectedTrack.url}
            controls
            autoPlay
          />
        </div>
      )}
    </div>
  );
}

export default App;

