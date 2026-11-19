import { useCallback, useEffect, useRef, useState } from "react";
import type { DriverLinkingApi } from "../api/driverLinkingApi";
import type {
  AccountSearchStatus,
  DriverAccount,
  NewDriverPayload,
  OrganizationDriver,
  ProviderProfile,
  RequestStatus,
  VerificationStatus,
} from "../types";

export function useDriverLinking(api: DriverLinkingApi) {
  const [profileError, setProfileError] = useState<string | null>(null);
  const [profile, setProfile] = useState<ProviderProfile | null>(null);

  // Busca de conta existente
  const [identifier, updateIdentifier] = useState("");
  const [foundAccount, setFoundAccount] = useState<DriverAccount | null>(null);
  const [searchStatus, setSearchStatus] = useState<AccountSearchStatus>("idle");
  const [isLinking, setIsLinking] = useState(false);
  const [linkError, setLinkError] = useState<string | null>(null);
  const searchRequest = useRef<AbortController | null>(null);
  const listRequest = useRef<AbortController | null>(null);

  // Motoristas da organização
  const [teamQuery, setTeamQuery] = useState("");
  const [drivers, setDrivers] = useState<OrganizationDriver[]>([]);
  const [driversStatus, setDriversStatus] = useState<RequestStatus>("loading");

  const reloadDrivers = useCallback(
    async (search?: string) => {
      listRequest.current?.abort();
      const controller = new AbortController();
      listRequest.current = controller;
      setDriversStatus("loading");
      try {
        const list = await api.listOrganizationDrivers(search, controller.signal);
        if (controller.signal.aborted) return;
        setDrivers(list);
        setDriversStatus("ready");
      } catch {
        if (!controller.signal.aborted) setDriversStatus("error");
      }
    },
    [api],
  );

  useEffect(() => {
    const controller = new AbortController();
    api
      .getProfile(controller.signal)
      .then(profile => {
        if (!controller.signal.aborted) { setProfile(profile); setProfileError(null); }
      })
      .catch((error) => {
        if (!controller.signal.aborted) setProfileError(error instanceof Error ? error.message : "Não foi possível carregar a organização.");
      });
    listRequest.current = controller;
    api.listOrganizationDrivers(undefined, controller.signal)
      .then(list => {
        if (controller.signal.aborted) return;
        setDrivers(list);
        setDriversStatus("ready");
      })
      .catch(() => {
        if (!controller.signal.aborted) setDriversStatus("error");
      });
    return () => { controller.abort(); searchRequest.current?.abort(); listRequest.current?.abort(); };
  }, [api]);

  const setIdentifier = useCallback((value: string) => {
    searchRequest.current?.abort();
    updateIdentifier(value);
    setFoundAccount(null);
    setSearchStatus("idle");
    setLinkError(null);
  }, []);

  const searchAccount = useCallback(async () => {
    const value = identifier.trim();
    if (!value) return;
    searchRequest.current?.abort();
    const controller = new AbortController();
    searchRequest.current = controller;
    setSearchStatus("loading");
    setFoundAccount(null);
    setLinkError(null);
    try {
      const account = await api.findAccount(value, controller.signal);
      if (controller.signal.aborted) return;
      setFoundAccount(account);
      setSearchStatus(account ? "found" : "notFound");
    } catch {
      if (!controller.signal.aborted) setSearchStatus("error");
    }
  }, [api, identifier]);

  const linkFoundAccount = useCallback(async () => {
    if (!foundAccount) return;
    setIsLinking(true);
    setLinkError(null);
    try {
      await api.linkDriver(foundAccount.id);
      setFoundAccount(null);
      setSearchStatus("idle");
      setIdentifier("");
      await reloadDrivers(teamQuery.trim() || undefined);
    } catch (error) {
      setLinkError(error instanceof Error ? error.message : "Não foi possível vincular o motorista.");
    } finally {
      setIsLinking(false);
    }
  }, [api, foundAccount, reloadDrivers, setIdentifier, teamQuery]);

  const searchTeam = useCallback(
    () => reloadDrivers(teamQuery.trim() || undefined),
    [reloadDrivers, teamQuery],
  );

  /** Lança erro em caso de falha, para o modal exibir a mensagem. */
  const createDriver = useCallback(
    async (payload: NewDriverPayload) => {
      await api.createDriver(payload);
      await reloadDrivers(teamQuery.trim() || undefined);
    },
    [api, reloadDrivers, teamQuery],
  );

  const verifyDriver = useCallback(async (id: string, status: VerificationStatus) => {
    await api.verifyDriver(id, status);
    await reloadDrivers(teamQuery.trim() || undefined);
  }, [api, reloadDrivers, teamQuery]);

  return {
    profileError,
    verifyDriver,
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
