import React from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AccountPage, LoginPage, RecoveryPage, RegisterPage, ResetPage } from "../features/auth/AuthPages";
import Home from "../pages/Home";

export const Router: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/entrar" element={<LoginPage />} />
        <Route path="/cadastro" element={<RegisterPage />} />
        <Route path="/recuperar-senha" element={<RecoveryPage />} />
        <Route path="/redefinir-senha" element={<ResetPage />} />
        <Route path="/conta" element={<AccountPage />} />
      </Routes>
    </BrowserRouter>
  );
};

export default Router;
