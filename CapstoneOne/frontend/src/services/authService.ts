import api from "@/lib/axios";

export interface LoginResponse {
  message: string;
  token: string;
  tokenType: string;
}

export interface AuthResponse {
  message: string;
  username: string;
  authenticated: boolean;
}

export interface MessageResponse {
  message: string;
}

export interface SignupRequest {
  username: string;
  email: string;
  password: string;
}

export async function login(
  username: string,
  password: string
): Promise<LoginResponse> {
  const response = await api.post<LoginResponse>("/login", {
    username,
    password,
  });

  return response.data;
}

export async function signup(
  data: SignupRequest
): Promise<MessageResponse> {
  const response = await api.post<MessageResponse>(
    "/signup",
    data
  );

  return response.data;
}

export async function refreshToken(): Promise<LoginResponse> {
  const response = await api.post<LoginResponse>("/refresh");

  return response.data;
}

export async function getAuthenticatedUser(): Promise<AuthResponse> {
  const response = await api.get<AuthResponse>("/auth");

  return response.data;
}

export async function logout(): Promise<MessageResponse> {
  const response = await api.post<MessageResponse>("/logout");

  return response.data;
}