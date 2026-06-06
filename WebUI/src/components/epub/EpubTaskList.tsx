import React from 'react';
import { useQuery, useMutation, gql } from '@apollo/client';

const GET_EBOOK_TASKS = gql`
  query GetEpubTasks($limit: Int) {
    epubTasks(limit: $limit) {
      id
      mangaId
      status
      createdAt
      completedAt
      errorMessage
    }
  }
`;

const DELETE_EBOOK_TASK = gql`
  mutation DeleteEpubTask($taskId: Int!) {
    deleteEpubTask(taskId: $taskId)
  }
`;

interface EpubTask {
  id: number;
  mangaId: number;
  status: string;
  createdAt: number;
  completedAt?: number;
  errorMessage?: string;
}

export const EpubTaskList: React.FC = () => {
  const { loading, error, data, refetch } = useQuery(GET_EBOOK_TASKS, {
    variables: { limit: 50 }
  });
  const [deleteTask] = useMutation(DELETE_EBOOK_TASK);

  const handleDelete = async (taskId: number) => {
    if (window.confirm('确定要删除这个任务吗？')) {
      await deleteTask({ variables: { taskId } });
      refetch();
    }
  };

  const getStatusBadge = (status: string) => {
    const styles: Record<string, { bg: string; color: string }> = {
      pending: { bg: '#21262d', color: '#8b949e' },
      downloading: { bg: '#1f6feb', color: 'white' },
      processing: { bg: '#1f6feb', color: 'white' },
      packaging: { bg: '#1f6feb', color: 'white' },
      done: { bg: '#238636', color: 'white' },
      error: { bg: '#da3633', color: 'white' },
    };
    const style = styles[status] || styles.pending;
    return (
      <span style={{ ...style, padding: '2px 8px', borderRadius: '12px', fontSize: '12px' }}>
        {status.toUpperCase()}
      </span>
    );
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;

  return (
    <div style={{ padding: '20px' }}>
      <h3>EPUB 导出任务</h3>
      <div style={{ border: '1px solid #30363d', borderRadius: '6px' }}>
        {data.epubTasks.map((task: EpubTask) => (
          <div key={task.id} style={{ padding: '12px 16px', borderBottom: '1px solid #30363d' }}>
            <span>任务 #{task.id}</span>
            <span style={{ marginLeft: '12px' }}>{getStatusBadge(task.status)}</span>
            <span style={{ marginLeft: '12px', color: '#8b949e' }}>
              {new Date(task.createdAt * 1000).toLocaleString()}
            </span>
            <button
              onClick={() => handleDelete(task.id)}
              style={{ marginLeft: '12px', background: '#da3633', color: 'white', border: 'none', padding: '4px 8px', borderRadius: '4px' }}
            >
              删除
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};
