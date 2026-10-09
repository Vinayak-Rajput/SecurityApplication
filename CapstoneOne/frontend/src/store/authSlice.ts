import { createSlice, PayloadAction } from "@reduxjs/toolkit";

interface AuthState {
  token: string | null;
  isAuthenticated: boolean;
  isCheckingAuth: boolean;
  user: string | null;
}

const initialState: AuthState = {
  token: null,
  isAuthenticated: false,
  isCheckingAuth: true,
  user: null,
};

const authSlice = createSlice({
  name: "auth",
  initialState,

  reducers: {
    loginSuccess: (
      state,
      action: PayloadAction<{
        token: string;
        user: string | null;
      }>
    ) => {
      state.token = action.payload.token;
      state.isAuthenticated = true;
      state.isCheckingAuth = false;
      state.user = action.payload.user;
    },

    setUser: (state, action: PayloadAction<string>) => {
      state.user = action.payload;
      state.isAuthenticated = true;
      state.isCheckingAuth = false;
    },

    logout: (state) => {
      state.token = null;
      state.isAuthenticated = false;
      state.isCheckingAuth = false;
      state.user = null;
    },
  },
});

export const {
  loginSuccess,
  setUser,
  logout,
} = authSlice.actions;

export default authSlice.reducer;