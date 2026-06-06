import React from 'react';

interface Chapter {
  chapterId: number;
  title: string;
  included: boolean;
  sortOrder: number;
}

interface Props {
  chapters: Chapter[];
  onToggle: (chapterId: number) => void;
  onReorder: (fromIndex: number, toIndex: number) => void;
}

export const EpubChapterPreview: React.FC<Props> = ({ chapters, onToggle, onReorder }) => {
  return (
    <div style={{ padding: '16px' }}>
      <h4>章节预览</h4>
      {chapters.map((chapter, index) => (
        <div
          key={chapter.chapterId}
          style={{
            display: 'flex',
            alignItems: 'center',
            padding: '8px',
            borderBottom: '1px solid #21262d',
            background: chapter.included ? '#161b22' : '#0d1117',
          }}
        >
          <input
            type="checkbox"
            checked={chapter.included}
            onChange={() => onToggle(chapter.chapterId)}
            style={{ marginRight: '8px' }}
          />
          <span style={{ flex: 1, color: '#e6e6e6' }}>{chapter.title}</span>
          <span style={{ color: '#8b949e', fontSize: '12px' }}>#{chapter.sortOrder}</span>
        </div>
      ))}
    </div>
  );
};
