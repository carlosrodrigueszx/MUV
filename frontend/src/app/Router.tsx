import React from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AccountPage, LoginPage, RecoveryPage, RegisterPage, ResetPage } from "../features/auth/AuthPages";
import Home from "../pages/Home";
import Vehicles from "../pages/Vehicles";
import ClientePrestadores from "../pages/ClientePrestadores";

export const Router: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/cliente/prestadores" element={<ClientePrestadores />} />
        <Route path="/entrar" element={<LoginPage />} />
        <Route path="/cadastro" element={<RegisterPage />} />
        <Route path="/recuperar-senha" element={<RecoveryPage />} />
        <Route path="/redefinir-senha" element={<ResetPage />} />
        <Route path="/conta" element={<AccountPage />} />
        <Route path="/prestador/veiculos" element={<Vehicles />} />
      </Routes>
    </BrowserRouter>
  );
};

export default Router;
