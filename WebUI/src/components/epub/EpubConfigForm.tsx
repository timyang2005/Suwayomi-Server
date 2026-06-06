import React from 'react';

interface EpubConfig {
  title: string;
  author: string;
  language: string;
  pageSize: string;
  imageQuality: number;
  stripWhitespace: boolean;
  groupBy: string;
  chaptersPerBook: number;
}

interface Props {
  config: EpubConfig;
  onChange: (config: EpubConfig) => void;
}

export const EpubConfigForm: React.FC<Props> = ({ config, onChange }) => {
  const handleChange = (field: keyof EpubConfig, value: any) => {
    onChange({ ...config, [field]: value });
  };

  return (
    <div style={{ padding: '16px' }}>
      <h4>EPUB 配置</h4>

      <div style={{ marginBottom: '16px' }}>
        <label style={{ display: 'block', marginBottom: '4px' }}>书名</label>
        <input
          type="text"
          value={config.title}
          onChange={(e) => handleChange('title', e.target.value)}
          style={{ width: '100%', padding: '8px', border: '1px solid #30363d', borderRadius: '4px', background: '#0d1117', color: '#e6e6e6' }}
        />
      </div>

      <div style={{ marginBottom: '16px' }}>
        <label style={{ display: 'block', marginBottom: '4px' }}>作者</label>
        <input
          type="text"
          value={config.author}
          onChange={(e) => handleChange('author', e.target.value)}
          style={{ width: '100%', padding: '8px', border: '1px solid #30363d', borderRadius: '4px', background: '#0d1117', color: '#e6e6e6' }}
        />
      </div>

      <div style={{ marginBottom: '16px' }}>
        <label style={{ display: 'block', marginBottom: '4px' }}>分组策略</label>
        <select
          value={config.groupBy}
          onChange={(e) => handleChange('groupBy', e.target.value)}
          style={{ width: '100%', padding: '8px', border: '1px solid #30363d', borderRadius: '4px', background: '#0d1117', color: '#e6e6e6' }}
        >
          <option value="VOLUME">按卷分组</option>
          <option value="CHAPTER_RANGE">按章节数分组</option>
          <option value="SINGLE">整本打包</option>
        </select>
      </div>

      <div style={{ marginBottom: '16px' }}>
        <label style={{ display: 'block', marginBottom: '4px' }}>页面尺寸</label>
        <select
          value={config.pageSize}
          onChange={(e) => handleChange('pageSize', e.target.value)}
          style={{ width: '100%', padding: '8px', border: '1px solid #30363d', borderRadius: '4px', background: '#0d1117', color: '#e6e6e6' }}
        >
          <option value="AUTO">自动</option>
          <option value="KINDLE_PW">Kindle Paperwhite</option>
          <option value="KINDLE_OASIS">Kindle Oasis</option>
          <option value="KOBO">Kobo</option>
        </select>
      </div>

      <div style={{ marginBottom: '16px' }}>
        <label style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <input
            type="checkbox"
            checked={config.stripWhitespace}
            onChange={(e) => handleChange('stripWhitespace', e.target.checked)}
          />
          自动裁切白边
        </label>
      </div>
    </div>
  );
};
