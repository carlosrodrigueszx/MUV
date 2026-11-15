import { useCallback, useEffect, useState } from "react";
import type { DriverLinkingApi } from "../api/driverLinkingApi";
import type {
  AccountSearchStatus,
  DriverAccount,
  NewDriverPayload,
  OrganizationDriver,
  ProviderProfile,
  RequestStatus,
} from "../types";

export function useDriverLinking(api: DriverLinkingApi) {
  const [profile, setProfile] = useState<ProviderProfile | null>(null);

  // Busca de conta existente
  const [identifier, setIdentifier] = useState("");
  const [foundAccount, setFoundAccount] = useState<DriverAccount | null>(null);
  const [searchStatus, setSearchStatus] = useState<AccountSearchStatus>("idle");
  const [isLinking, setIsLinking] = useState(false);
  const [linkError, setLinkError] = useState(false);

  // Motoristas da organização
  const [teamQuery, setTeamQuery] = useState("");
  const [drivers, setDrivers] = useState<OrganizationDriver[]>([]);
  const [driversStatus, setDriversStatus] = useState<RequestStatus>("idle");

  const loadDrivers = useCallback(
    async (search?: string, signal?: AbortSignal) => {
      setDriversStatus("loading");
      try {
        const list = await api.listOrganizationDrivers(search, signal);
        setDrivers(list);
        setDriversStatus("ready");
      } catch {
        if (signal?.aborted) return;
        setDriversStatus("error");
      }
    },
    [api],
  );

  useEffect(() => {
    const controller = new AbortController();
    api
      .getProfile(controller.signal)
      .then(setProfile)
      .catch(() => {
        /* sem perfil: o layout renderiza com valores vazios */
      });
    void loadDrivers(undefined, controller.signal);
    return () => controller.abort();
  }, [api, loadDrivers]);

  const searchAccount = useCallback(async () => {
    const value = identifier.trim();
    if (!value) return;
    setSearchStatus("loading");
    setFoundAccount(null);
    setLinkError(false);
    try {
      const account = await api.findAccount(value);
      setFoundAccount(account);
      setSearchStatus(account ? "found" : "notFound");
    } catch {
      setSearchStatus("error");
    }
  }, [api, identifier]);

  const linkFoundAccount = useCallback(async () => {
    if (!foundAccount) return;
    setIsLinking(true);
    setLinkError(false);
    try {
      await api.linkDriver(foundAccount.id);
      setFoundAccount(null);
      setSearchStatus("idle");
      setIdentifier("");
      await loadDrivers(teamQuery.trim() || undefined);
    } catch {
      setLinkError(true);
    } finally {
      setIsLinking(false);
    }
  }, [api, foundAccount, loadDrivers, teamQuery]);

  const searchTeam = useCallback(
    () => loadDrivers(teamQuery.trim() || undefined),
    [loadDrivers, teamQuery],
  );

  /** Lança erro em caso de falha, para o modal exibir a mensagem. */
  const createDriver = useCallback(
    async (payload: NewDriverPayload) => {
      await api.createDriver(payload);
      await loadDrivers(teamQuery.trim() || undefined);
    },
    [api, loadDrivers, teamQuery],
  );

  return {
    profile,
    identifier,
    setIdentifier,
    foundAccount,
    searchStatus,
    searchAccount,
    isLinking,
    linkError,
    linkFoundAccount,
    teamQuery,
    setTeamQuery,
    drivers,
    driversStatus,
    searchTeam,
    createDriver,
  };
}
