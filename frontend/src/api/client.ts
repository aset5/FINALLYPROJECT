const defaultOptions: RequestInit = {
  credentials: 'include',
  headers: { Accept: 'application/json' },
};

async function handleResponse<T>(res: Response): Promise<T> {
  if (res.status === 204) {
    return undefined as T;
  }
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const message = data?.message || res.statusText;
    throw new Error(message);
  }
  return data as T;
}

export const api = {
  get: <T>(url: string) =>
    fetch(url, { ...defaultOptions, method: 'GET' }).then((r) => handleResponse<T>(r)),

  post: <T>(url: string, body?: unknown) =>
    fetch(url, {
      ...defaultOptions,
      method: 'POST',
      headers: {
        ...defaultOptions.headers,
        ...(body instanceof URLSearchParams
          ? { 'Content-Type': 'application/x-www-form-urlencoded' }
          : body !== undefined
            ? { 'Content-Type': 'application/json' }
            : {}),
      },
      body:
        body instanceof URLSearchParams
          ? body
          : body !== undefined
            ? JSON.stringify(body)
            : undefined,
    }).then((r) => handleResponse<T>(r)),

  put: <T>(url: string, body?: unknown) =>
    fetch(url, {
      ...defaultOptions,
      method: 'PUT',
      headers: { ...defaultOptions.headers, 'Content-Type': 'application/json' },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    }).then((r) => handleResponse<T>(r)),

  delete: (url: string) =>
    fetch(url, { ...defaultOptions, method: 'DELETE' }).then((r) => handleResponse<void>(r)),

  postForm: <T>(url: string, formData: FormData) =>
    fetch(url, { credentials: 'include', method: 'POST', body: formData }).then((r) =>
      handleResponse<T>(r),
    ),

  putForm: <T>(url: string, formData: FormData) =>
    fetch(url, { credentials: 'include', method: 'PUT', body: formData }).then((r) =>
      handleResponse<T>(r),
    ),
};

export async function login(username: string, password: string) {
  const body = new URLSearchParams({ username, password });
  const res = await fetch('/api/auth/login', {
    credentials: 'include',
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body,
  });
  if (!res.ok) {
    const text = await res.text();
    let message = 'Неверный логин или пароль';
    try {
      const data = text ? JSON.parse(text) : null;
      if (data?.message) message = data.message;
    } catch {
      /* ignore */
    }
    throw new Error(message);
  }
}

export async function logout() {
  return api.post<void>('/api/auth/logout');
}

export async function fetchMe() {
  return api.get<import('../types').User>('/api/auth/me');
}

function parseFilename(contentDisposition: string | null): string | undefined {
  if (!contentDisposition) return undefined;
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match) {
    return decodeURIComponent(utf8Match[1]);
  }
  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
  return plainMatch?.[1];
}

/** Скачивание бинарного файла (резюме и т.д.) с учётом сессии */
export async function downloadFile(url: string, fallbackName = 'resume') {
  const res = await fetch(url, { credentials: 'include' });
  if (!res.ok) {
    let message = 'Не удалось скачать файл';
    try {
      const data = await res.json();
      if (data?.message) message = data.message;
    } catch {
      /* бинарный ответ */
    }
    throw new Error(message);
  }
  const blob = await res.blob();
  const filename = parseFilename(res.headers.get('Content-Disposition')) || fallbackName;
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(link.href);
}
