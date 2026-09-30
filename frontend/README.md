# SyncSphere Frontend

React 19 + Vite + React Router + Axios.

## Run

1. Start the Spring Boot backend on http://localhost:8080.
2. In this folder:

```
npm install
npm run dev
```

Open http://localhost:5173.

Vite proxies `/api` to `http://localhost:8080` (see `vite.config.js`), so no CORS setup is needed in development. To point at a different backend, change the proxy target.

## Structure

- `src/services/api.js` - single Axios instance, attaches the JWT, redirects to login on 401
- `src/services/authService.js`, `fileService.js` - API calls
- `src/hooks` - `useAuth`, `useFiles`
- `src/components` - Navbar, Sidebar, UploadForm, FileCard, FileTable, ProtectedRoute, LoadingSpinner
- `src/pages` - Login, Register, Dashboard, NotFound

## Backend contract

- `POST /api/auth/login` returns `{ "token": "..." }`
- `GET /api/files` returns an array of files with `fileName`, `fileSize`, `uploadTime` (also accepts `filename`/`name`, `size`, `uploadedAt`)
- Download and delete use the file name: `/api/files/download/{filename}`, `/api/files/delete/{filename}`
