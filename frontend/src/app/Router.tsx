import React from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import {
  AccountPage,
  LoginPage,
  RecoveryPage,
  RegisterPage,
  ResetPage,
} from "../features/auth/AuthPages";
import FreightCancellationPage from "../features/freight-cancellation/FreightCancellationPage";
import Home from "../pages/Home";
import Vehicles from "../pages/Vehicles";
import DriverLinking from "../pages/DriverLinking";
import FreightHistory from "../pages/FreightHistory";
import ClientePrestadores from "../pages/ClientePrestadores";
import PlanejarViagem from "../pages/PlanejarViagem";

export const Router: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/cliente/prestadores" element={<ClientePrestadores />} />
        <Route path="/cliente/planejar-viagem" element={<PlanejarViagem />} />
        <Route path="/entrar" element={<LoginPage />} />
        <Route path="/cadastro" element={<RegisterPage />} />
        <Route path="/recuperar-senha" element={<RecoveryPage />} />
        <Route path="/redefinir-senha" element={<ResetPage />} />
        <Route path="/fretes/:freteId/cancelar" element={<FreightCancellationPage />} />
        <Route path="/conta" element={<AccountPage />} />
        <Route path="/prestador/veiculos" element={<Vehicles />} />
        <Route path="/prestador/motoristas" element={<DriverLinking />} />
        <Route path="/prestador/historico" element={<FreightHistory />} />
        <Route path="/cliente/historico" element={<FreightHistory />} />
      </Routes>
    </BrowserRouter>
  );
};

export default Router;
