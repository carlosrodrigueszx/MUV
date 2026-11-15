import { useMemo, useState } from "react";
import { ProviderLayout } from "../components/layout/ProviderLayout";
import {
  AddDriverModal,
  AddDriverSection,
  OrganizationDriversSection,
  createDriverLinkingApi,
  useDriverLinking,
  type DriverLinkingApi,
} from "../features/driver-linking";

interface DriverLinkingProps {
  /** Permite injetar outra implementação da API (testes, mocks, etc.). */
  api?: DriverLinkingApi;
}

export default function DriverLinking({ api: injectedApi }: DriverLinkingProps) {
  const api = useMemo(
    () =>
      injectedApi ??
      createDriverLinkingApi({ baseUrl: import.meta.env.VITE_API_URL ?? "" }),
    [injectedApi],
  );
  const driverLinking = useDriverLinking(api);
  const [modalOpen, setModalOpen] = useState(false);
  const { profile } = driverLinking;

  return (
    <ProviderLayout
      pageTitle="Vincular e verificar motorista"
      activeNavKey="drivers"
      organizationName={profile?.organizationName}
      organizationVerified={profile?.verified}
      avatarUrl={profile?.avatarUrl}
      isOnline={profile?.isOnline}
      navBadges={{ requests: profile?.pendingRequestsCount ?? 0 }}
    >
      <div className="flex flex-col gap-6">
        <h1 className="text-[30px] leading-[1.45] font-bold text-[#1d2d32]">
          Sua equipe, vinculada e verificada
        </h1>
        <p className="text-[14px] leading-5 text-[#637479]">
          {profile?.organizationName ? `${profile.organizationName} · ` : ""}Acesso do gestor da
          organização
        </p>

        <div className="flex flex-col gap-5">
          <AddDriverSection
            identifier={driverLinking.identifier}
            onIdentifierChange={driverLinking.setIdentifier}
            onSearch={driverLinking.searchAccount}
            onNewDriver={() => setModalOpen(true)}
            status={driverLinking.searchStatus}
            account={driverLinking.foundAccount}
            onLink={driverLinking.linkFoundAccount}
            isLinking={driverLinking.isLinking}
            linkError={driverLinking.linkError}
          />
          <OrganizationDriversSection
            query={driverLinking.teamQuery}
            onQueryChange={driverLinking.setTeamQuery}
            onSearch={driverLinking.searchTeam}
            drivers={driverLinking.drivers}
            status={driverLinking.driversStatus}
          />
        </div>
      </div>

      <AddDriverModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSubmit={driverLinking.createDriver}
      />
    </ProviderLayout>
  );
}
