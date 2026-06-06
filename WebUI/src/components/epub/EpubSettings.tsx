import React from 'react';

export const EpubSettings: React.FC = () => {
  return (
    <div style={{ padding: '20px' }}>
      <h3>EPUB 全局设置</h3>
      <div style={{ marginBottom: '16px' }}>
        <label style={{ display: 'block', marginBottom: '4px' }}>默认输出目录</label>
        <input
          type="text"
          defaultValue="~/epub-output/"
          style={{ width: '100%', padding: '8px', border: '1px solid #30363d', borderRadius: '4px', background: '#0d1117', color: '#e6e6e6' }}
        />
      </div>
      <div style={{ marginBottom: '16px' }}>
        <label style={{ display: 'block', marginBottom: '4px' }}>默认页面尺寸</label>
        <select style={{ width: '100%', padding: '8px', border: '1px solid #30363d', borderRadius: '4px', background: '#0d1117', color: '#e6e6e6' }}>
          <option value="AUTO">自动</option>
          <option value="KINDLE_PW">Kindle Paperwhite</option>
          <option value="KINDLE_OASIS">Kindle Oasis</option>
          <option value="KOBO">Kobo</option>
        </select>
      </div>
    </div>
  );
};
