import React from "react";
import PortalOverview from "../pages/PortalOverview";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import {
  AccountPage,
  LoginPage,
  RecoveryPage,
  RegisterPage,
  ResetPage,
} from "../features/auth/AuthPages";
import FreightCancellationPage from "../features/freight-cancellation/FreightCancellationPage";
import PaginaAvaliacao from "../features/propostas/Avaliacao";
import Disponiveis from "../features/solicitacoes/Disponiveis";
import { CompararPropostas, FreteContratado } from "../features/contratacao/Contratacao";
import Home from "../pages/Home";
import Vehicles from "../pages/Vehicles";
import DriverLinking from "../pages/DriverLinking";
import FreightHistory from "../pages/FreightHistory";
import ClientePrestadores from "../pages/ClientePrestadores";
import PlanejarViagem from "../pages/PlanejarViagem";
import {
  DesignarRecursosPage,
  SubstituirRecursosPage,
} from "../features/designacao/DesignacaoPages";
import { AchadosPerdidosPage, RegistrarAchadoPage } from "../features/achados-perdidos/AchadosPerdidosPages";

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
        <Route path="/prestador/solicitacoes/:id/avaliacao" element={<PaginaAvaliacao />} />
        <Route path="/prestador" element={<PortalOverview />} />
        <Route path="/motorista" element={<PortalOverview driver />} />
        <Route path="/motorista/solicitacoes" element={<Disponiveis />} />
        <Route path="/motorista/solicitacoes/:id/avaliacao" element={<PaginaAvaliacao />} />
        <Route path="/motorista/veiculos" element={<Vehicles />} />
        <Route path="/prestador/solicitacoes" element={<Disponiveis />} />
        <Route path="/prestador/veiculos" element={<Vehicles />} />
        <Route path="/prestador/motoristas" element={<DriverLinking />} />
        <Route path="/prestador/historico" element={<FreightHistory />} />
        <Route path="/prestador/achados-perdidos" element={<AchadosPerdidosPage />} />
        <Route path="/prestador/achados-perdidos/novo" element={<RegistrarAchadoPage />} />
        <Route path="/motorista/achados-perdidos" element={<AchadosPerdidosPage />} />
        <Route path="/motorista/achados-perdidos/novo" element={<RegistrarAchadoPage />} />
        <Route path="/prestador/fretes/:id/designacao" element={<DesignarRecursosPage />} />
        <Route path="/prestador/fretes/:id/substituicao" element={<SubstituirRecursosPage />} />
        <Route path="/cliente/solicitacoes/:id/propostas" element={<CompararPropostas />} />
        <Route path="/cliente/fretes/:id" element={<FreteContratado />} />
        <Route path="/cliente/historico" element={<FreightHistory />} />
      </Routes>
    </BrowserRouter>
  );
};

export default Router;
