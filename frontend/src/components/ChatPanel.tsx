import { useEffect, useRef, type FormEvent } from 'react';
import { Button, Form } from 'react-bootstrap';
import type { Message } from '../types';

export type ChatPeerKind = 'university' | 'company' | 'student';

export interface ChatPanelProps {
  messages: Message[];
  currentUsername?: string;
  currentUserId?: number;
  peerName: string;
  peerSubtitle?: string;
  peerKind?: ChatPeerKind;
  inputValue: string;
  onInputChange: (value: string) => void;
  onSubmit: (e: FormEvent) => void;
  placeholder?: string;
  disabled?: boolean;
  emptyHint?: string;
  sending?: boolean;
}

const PEER_ICONS: Record<ChatPeerKind, string> = {
  university: 'bi-mortarboard-fill',
  company: 'bi-building-fill',
  student: 'bi-person-fill',
};

function isOwnMessage(m: Message, currentUsername?: string, currentUserId?: number): boolean {
  if (currentUserId != null) return m.senderId === currentUserId;
  if (currentUsername) return m.senderUsername === currentUsername;
  return false;
}

function formatMessageTime(sentAt: string): string {
  try {
    const d = new Date(sentAt);
    if (Number.isNaN(d.getTime())) return '';
    const now = new Date();
    const sameDay =
      d.getFullYear() === now.getFullYear() &&
      d.getMonth() === now.getMonth() &&
      d.getDate() === now.getDate();
    const time = d.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
    if (sameDay) return time;
    return d.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' }) + ', ' + time;
  } catch {
    return '';
  }
}

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return (name.slice(0, 2) || '?').toUpperCase();
}

export default function ChatPanel({
  messages,
  currentUsername,
  currentUserId,
  peerName,
  peerSubtitle,
  peerKind = 'company',
  inputValue,
  onInputChange,
  onSubmit,
  placeholder = 'Напишите сообщение...',
  disabled = false,
  emptyHint = 'Начните диалог — напишите первое сообщение',
  sending = false,
}: ChatPanelProps) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    el.scrollTop = el.scrollHeight;
  }, [messages]);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (disabled || sending || !inputValue.trim()) return;
    onSubmit(e);
    requestAnimationFrame(() => inputRef.current?.focus());
  };

  return (
    <div className="chat-panel">
      <header className="chat-panel__header">
        <div className={`chat-panel__avatar chat-panel__avatar--${peerKind}`}>
          <i className={`bi ${PEER_ICONS[peerKind]}`} aria-hidden />
        </div>
        <div className="chat-panel__header-text">
          <h3 className="chat-panel__peer-name">{peerName}</h3>
          {peerSubtitle && <p className="chat-panel__peer-sub">{peerSubtitle}</p>}
        </div>
        <span className="chat-panel__badge">
          <span className="chat-panel__badge-dot" aria-hidden />
          Онлайн-чат
        </span>
      </header>

      <div className="chat-panel__messages" ref={scrollRef} role="log" aria-live="polite">
        {messages.length === 0 ? (
          <div className="chat-panel__empty">
            <div className="chat-panel__empty-icon">
              <i className="bi bi-chat-heart" aria-hidden />
            </div>
            <p className="chat-panel__empty-title">Пока нет сообщений</p>
            <p className="chat-panel__empty-hint">{emptyHint}</p>
          </div>
        ) : (
          <div className="chat-panel__thread">
            {messages.map((m, i) => {
              const own = isOwnMessage(m, currentUsername, currentUserId);
              const prev = messages[i - 1];
              const showSender =
                !own && (!prev || prev.senderUsername !== m.senderUsername || isOwnMessage(prev, currentUsername, currentUserId));
              const time = formatMessageTime(m.sentAt);

              return (
                <div
                  key={m.id}
                  className={`chat-bubble-row ${own ? 'chat-bubble-row--own' : 'chat-bubble-row--peer'}`}
                >
                  {!own && showSender && (
                    <div className="chat-bubble-row__sender">
                      <span className="chat-bubble-row__avatar-mini">{initials(m.senderUsername)}</span>
                      <span>{m.senderUsername}</span>
                    </div>
                  )}
                  <div className={`chat-bubble ${own ? 'chat-bubble--own' : 'chat-bubble--peer'}`}>
                    <p className="chat-bubble__text">{m.content}</p>
                    {time && (
                      <time className="chat-bubble__time" dateTime={m.sentAt}>
                        {time}
                        {own && (
                          <i className="bi bi-check2-all chat-bubble__read" aria-label="Отправлено" />
                        )}
                      </time>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <footer className="chat-panel__composer">
        <Form onSubmit={handleSubmit} className="chat-panel__form">
          <div className="chat-panel__input-wrap">
            <Form.Control
              ref={inputRef}
              className="chat-panel__input"
              value={inputValue}
              onChange={(e) => onInputChange(e.target.value)}
              placeholder={placeholder}
              disabled={disabled || sending}
              autoComplete="off"
            />
          </div>
          <Button
            type="submit"
            className="chat-panel__send btn-gradient"
            disabled={disabled || sending || !inputValue.trim()}
            aria-label="Отправить"
          >
            {sending ? (
              <span className="spinner-border spinner-border-sm" role="status" />
            ) : (
              <i className="bi bi-send-fill" />
            )}
          </Button>
        </Form>
      </footer>
    </div>
  );
}
