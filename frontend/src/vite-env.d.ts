/// <reference types="vite/client" />

interface ImportMetaEnv {
  // UUID of the user to treat as signed in until login exists; defaults to the System user (see useCurrentUser)
  // TODO(auth): remove once login exists (also from .env.example); the auth
  // provider's settings (e.g. its URL and public key) will likely go here instead
  readonly VITE_CURRENT_USER_ID?: string;
}
