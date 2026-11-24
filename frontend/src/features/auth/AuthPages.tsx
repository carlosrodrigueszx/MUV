import { useEffect, useId, useState, type ComponentProps, type FormEvent, type ReactNode } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { LuArrowRight, LuBus, LuEye, LuEyeOff, LuLockKeyhole, LuMail, LuUserRound } from "react-icons/lu";
import { authPost, getSession, type Session } from "./api";

const inputStyle = "w-full rounded-lg border border-slate-200 bg-slate-100 px-3 py-2.5 text-slate-900 focus-visible:outline-2 focus-visible:outline-[#1A5344]";
const buttonStyle = "rounded-lg bg-[#1A5344] px-4 py-2.5 font-semibold text-white hover:bg-[#144236] disabled:opacity-60";
const secondaryButtonStyle = "rounded-lg border border-slate-200 px-3 py-2 text-sm font-semibold text-[#1A5344] hover:bg-slate-50 disabled:text-slate-500 disabled:bg-slate-100";
const successStyle = "rounded-lg bg-[#e5f2ed] p-3 text-sm text-[#1A5344]";
const linkStyle = "font-semibold text-[#1A5344] underline underline-offset-2";

function Field({ label, secret, icon, ...props }: ComponentProps<"input"> & {
  label: string;
  secret?: boolean;
  icon?: ReactNode;
}) {
  const [visible, setVisible] = useState(false);
  const fieldId = useId();
  const id = props.id ?? fieldId;
  return <div className="block space-y-1 text-sm font-medium text-slate-700">
    <label htmlFor={id}>{label}</label>
    <span className="relative block">
      {icon && <span aria-hidden="true" className="pointer-events-none absolute inset-y-0 left-3 flex items-center text-slate-500">{icon}</span>}
      <input {...props} id={id} type={secret ? visible ? "text" : "password" : props.type}
        className={`${inputStyle} ${secret ? "pr-11" : ""} ${icon ? "pl-9" : ""}`} required />
      {secret && <button type="button" aria-label={visible ? "Ocultar senha" : "Mostrar senha"}
        className="absolute inset-y-0 right-0 px-3 text-slate-600" onClick={() => setVisible(!visible)}>
        {visible ? <LuEyeOff /> : <LuEye />}
      </button>}
    </span>
  </div>;
}

function AuthCard({ title, intro, wide, login, compact, children }: {
  title: string;
  intro: string;
  wide?: boolean;
  login?: boolean;
  compact?: boolean;
  children: ReactNode;
}) {
  return <main className="grid min-h-screen place-items-center bg-[#f4f6f7] px-4 py-10 text-slate-900">
    <section className={`w-full rounded-2xl border border-slate-200 bg-white p-6 shadow-lg sm:p-8 ${wide ? "max-w-2xl" : compact ? "max-w-sm sm:p-5" : "max-w-md"}`}>
      {wide
        ? <p className="text-sm text-slate-600">Já tem conta? <Link className={linkStyle} to="/entrar">Fazer login</Link></p>
        : !compact && <Link className={`text-sm ${linkStyle}`} to="/">← Voltar ao início</Link>}
      <div className={`my-6 ${login ? "text-center" : compact ? "!mt-0" : ""}`}>
        {login && <span className="mb-3 inline-grid size-10 place-items-center rounded-xl bg-[#1A5344] text-white"><LuBus aria-hidden="true" /></span>}
        <h1 className={compact ? "text-lg font-bold" : login ? "text-2xl font-bold" : "text-2xl font-bold text-[#1A5344]"}>{title}</h1>
        <p className="mt-1 text-sm text-slate-600">{intro}</p>
      </div>
      {children}
    </section>
  </main>;
}

function useSubmit(submit: (data: Record<string, string>) => Promise<void>) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const values = Object.fromEntries(new FormData(event.currentTarget)) as Record<string, string>;
    setBusy(true);
    setError("");
    try {
      await submit(values);
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : "Não foi possível concluir.");
    } finally {
      setBusy(false);
    }
  }
  return { busy, error, handleSubmit };
}

function Feedback({ error }: { error: string }) {
  return error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-800">{error}</p>;
}

function PasswordPair({ reset = false, provider = false }: { reset?: boolean; provider?: boolean }) {
  const helpId = useId();
  const matchId = useId();
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const passwordsMatch = password.length > 0 && password === confirmation;
  const passwordTooLong = new TextEncoder().encode(password).length > 72;

  return <>
    <div className="space-y-1">
      <Field name="senha" label={reset ? "Nova senha" : provider ? "Senha mestre operacional" : "Senha de acesso"} secret
        autoComplete="new-password" minLength={8} maxLength={72}
        value={password} onChange={event => setPassword(event.target.value)}
        aria-describedby={helpId} aria-invalid={passwordTooLong || undefined} />
      <p id={helpId} className={`text-xs ${passwordTooLong ? "text-red-800" : "text-slate-600"}`}>
        {passwordTooLong ? "A senha excede o limite de 72 bytes." : "Use pelo menos 8 caracteres."}
      </p>
    </div>
    <div className="space-y-1">
      <Field name="confirmarSenha" label={reset ? "Confirmar nova senha" : provider ? "Confirmar senha mestre" : "Confirmar senha"} secret
        autoComplete="new-password" minLength={8} maxLength={72}
        value={confirmation} onChange={event => setConfirmation(event.target.value)}
        aria-describedby={confirmation ? matchId : undefined}
        aria-invalid={confirmation.length > 0 && !passwordsMatch ? true : undefined} />
      {confirmation && <div id={matchId} role="status"
        className={passwordsMatch && reset ? successStyle : `text-xs ${passwordsMatch ? "text-[#1A5344]" : "text-red-800"}`}>
        <strong>{passwordsMatch ? "As senhas coincidem" : "As senhas não coincidem."}</strong>
        {passwordsMatch && reset && <p>Após salvar, use a nova senha em todos os seus perfis.</p>}
      </div>}
    </div>
  </>;
}

function validatePasswords(values: Record<string, string>) {
  if (values.senha !== values.confirmarSenha) throw new Error("As senhas não coincidem.");
  if (new TextEncoder().encode(values.senha).length > 72) {
    throw new Error("A senha excede o limite de 72 bytes.");
  }
}

export function LoginPage() {
  const navigate = useNavigate();
  const notice = useLocation().state?.notice as string | undefined;
  const { busy, error, handleSubmit } = useSubmit(async values => {
    await authPost("login", new URLSearchParams(values));
    const account = await getSession(new AbortController().signal);
    if (!account) throw new Error("Não foi possível confirmar a sessão. Entre novamente.");

    let destination = "/cliente/planejar-viagem";
    if (account.empresa) destination = "/prestador";
    else if (account.motorista || account.prestador) destination = "/motorista";
    navigate(destination, { replace: true });
  });

  return <AuthCard title="Portal de Acesso MUV" intro="Acesse sua conta MUV como passageiro ou prestador." login>
    {notice && <p role="status" className="mb-4 text-sm text-[#1A5344]">{notice}</p>}
    <form className="space-y-4" onSubmit={handleSubmit}>
      <Field name="email" label="E-mail cadastrado" placeholder="Digite seu e-mail" type="email" icon={<LuMail size={16} />} autoComplete="username" maxLength={255} />
      <Field name="senha" label="Senha de acesso" placeholder="Digite sua senha cadastrada" secret icon={<LuLockKeyhole size={16} />} autoComplete="current-password" maxLength={72} />
      <Link to="/recuperar-senha" className={`block text-right text-sm ${linkStyle}`}>Esqueceu a senha?</Link>
      <Feedback error={error} />
      <button className={`flex w-full items-center justify-center gap-2 ${buttonStyle}`} disabled={busy}>
        {busy ? "Entrando…" : "Entrar"} <LuArrowRight aria-hidden="true" size={16} />
      </button>
    </form>
    <div className="mt-5 flex flex-wrap items-center justify-between gap-3 rounded-lg bg-slate-100 p-3 text-sm">
      <p className="font-medium">Ainda não tem cadastro no MUV?</p>
      <Link to="/cadastro" className={buttonStyle}>Criar nova conta</Link>
    </div>
  </AuthCard>;
}

function CpfField() {
  return <Field name="cpf" label="CPF do titular" inputMode="numeric" placeholder="000.000.000-00" pattern={String.raw`[0-9]{11}|[0-9]{3}\.[0-9]{3}\.[0-9]{3}-[0-9]{2}`} title="Informe 11 dígitos, com ou sem pontuação." maxLength={14} />;
}

function GenderField() {
  return <label className="block space-y-1 text-sm font-medium text-slate-700">
    <span>Gênero / identidade</span>
    <select name="genero" className={inputStyle} defaultValue="" required>
      <option value="" disabled>Selecione</option>
      <option>Feminino</option><option>Masculino</option><option>Não binário</option>
      <option>Outro</option><option>Prefiro não informar</option>
    </select>
  </label>;
}

function TermsCheckbox() {
  return <label className="flex items-start gap-2">
    <input type="checkbox" name="aceitouTermos" required className="mt-1 accent-[#1A5344]" />
    <span>Concordo com os Termos de Uso Operacional do MUV e com a Política de Proteção e Tratamento de Dados.</span>
  </label>;
}

function LicenseFields() {
  return <>
    <Field name="cnh" label="CNH" inputMode="numeric" pattern="[0-9]{11}" maxLength={11} />
    <label className="block space-y-1 text-sm font-medium text-slate-700">
      <span>Categoria da CNH</span>
      <select name="categoriaCnh" className={inputStyle} defaultValue="" required>
        <option value="" disabled>Selecione</option>
        {["A", "B", "C", "D", "E", "AB", "AC", "AD", "AE"].map(category =>
          <option key={category}>{category}</option>)}
      </select>
    </label>
  </>;
}

function ComplianceCheckbox() {
  return <label className="flex items-start gap-2">
    <input type="checkbox" name="declarouConformidade" required className="mt-1 accent-[#1A5344]" />
    <span>Declaro a conformidade das informações prestadas com as normas vigentes (ARCE / ANTT).</span>
  </label>;
}

function ProviderRegistrationFields({ company, active }: { company: boolean; active: boolean }) {
  return <fieldset hidden={!active} disabled={!active} className={active ? "space-y-4" : "hidden"}>
      <div className="grid gap-4 sm:grid-cols-2">
        {company ? <>
          <Field name="razaoSocial" label="Razão social" autoComplete="organization" maxLength={255} />
          <Field name="nomeFantasia" label="Nome fantasia" maxLength={255} />
          <div className="grid gap-4 sm:col-span-2 sm:grid-cols-3">
            <Field name="cnpj" label="CNPJ" inputMode="numeric" placeholder="00.000.000/0000-00" pattern={String.raw`[0-9]{14}|[0-9]{2}\.[0-9]{3}\.[0-9]{3}\/[0-9]{4}-[0-9]{2}`} title="Informe 14 dígitos, com ou sem pontuação." maxLength={18} />
            <Field name="telefone" label="Telefone / WhatsApp" type="tel" autoComplete="tel" maxLength={20} />
            <Field name="email" label="E-mail profissional" type="email" autoComplete="email" maxLength={255} />
          </div>
        </> : <>
          <Field name="nome" label="Nome completo" autoComplete="name" maxLength={255} />
          <CpfField />
          <div className="grid gap-4 sm:col-span-2 sm:grid-cols-4">
            <Field name="nascimento" label="Data de nascimento" type="date" autoComplete="bday" />
            <GenderField />
            <LicenseFields />
          </div>
          <Field name="telefone" label="Celular / WhatsApp" type="tel" autoComplete="tel" maxLength={20} />
          <Field name="email" label="E-mail" type="email" autoComplete="email" maxLength={255} />
        </>}
        <PasswordPair provider />
      </div>
      <div className="space-y-2 rounded-lg bg-slate-100 p-3 text-sm text-slate-700">
        <ComplianceCheckbox />
        <TermsCheckbox />
      </div>
  </fieldset>;
}

export function RegisterPage() {
  const navigate = useNavigate();
  const [provider, setProvider] = useState(false);
  const [company, setCompany] = useState(true);
  const profile = provider ? company ? "empresa" : "autonomo" : "passageiro";
  const [submittedProfile, setSubmittedProfile] = useState<typeof profile>();
  const { busy, error, handleSubmit } = useSubmit(async values => {
    setSubmittedProfile(profile);
    validatePasswords(values);
    const { aceitouTermos, declarouConformidade, cnh, categoriaCnh, ...fields } = values;
    const registration = { ...fields, aceitouTermos: aceitouTermos === "on" };
    const compliance = declarouConformidade === "on";

    if (profile === "empresa") {
      await authPost("cadastro/empresa", { ...registration, declarouConformidade: compliance });
    } else if (profile === "autonomo") {
      await authPost("cadastro/autonomo", { conta: registration, cnh, categoriaCnh, declarouConformidade: compliance });
    } else {
      await authPost("cadastro", registration);
    }
    navigate("/entrar", { replace: true, state: { notice: "Conta criada. Entre com seu e-mail e senha." } });
  });

  const profileStyle = "flex items-center gap-3 rounded-xl px-4 py-3 text-left";
  const selectedStyle = "bg-[#1A5344] text-white shadow-md";
  const unselectedStyle = "bg-[#d5f3ef] text-[#1A5344]";

  return <AuthCard title="Crie sua Conta no MUV" intro="Selecione seu perfil para iniciar o cadastro." wide>
    <div role="group" aria-label="Perfil da conta" className="mb-5 grid gap-3 sm:grid-cols-2">
      <button type="button" aria-pressed={!provider} disabled={busy}
        className={`${profileStyle} ${provider ? unselectedStyle : selectedStyle}`} onClick={() => setProvider(false)}>
        <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-white text-[#1A5344]"><LuUserRound aria-hidden="true" /></span>
        <span><strong className="block">Passageiro</strong><span className="text-xs">Usuário comum</span></span>
      </button>
      <button type="button" aria-pressed={provider} disabled={busy}
        className={`${profileStyle} ${provider ? selectedStyle : unselectedStyle}`} onClick={() => setProvider(true)}>
        <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-white text-[#1A5344]"><LuBus aria-hidden="true" /></span>
        <span><strong className="block">Prestador</strong><span className="text-xs">Empresa ou autônomo</span></span>
      </button>
    </div>
    {provider && <div role="group" aria-label="Tipo de prestador" className="mb-4 flex justify-end gap-1">
      <button type="button" aria-pressed={company} disabled={busy}
        className={company ? buttonStyle : secondaryButtonStyle} onClick={() => setCompany(true)}>Empresa</button>
      <button type="button" aria-pressed={!company} disabled={busy}
        className={!company ? buttonStyle : secondaryButtonStyle} onClick={() => setCompany(false)}>Autônomo</button>
    </div>}
    <form className="space-y-4" onSubmit={handleSubmit}>
      <fieldset disabled={busy} className="space-y-4">
        <fieldset hidden={provider} disabled={provider} className={provider ? "hidden" : "space-y-4"}>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field name="nome" label="Nome completo" autoComplete="name" maxLength={255} />
            <Field name="nascimento" label="Data de nascimento" type="date" autoComplete="bday" />
            <CpfField />
            <GenderField />
            <Field name="email" label="E-mail principal" type="email" autoComplete="email" maxLength={255} />
            <Field name="telefone" label="Celular / WhatsApp" type="tel" autoComplete="tel" maxLength={20} />
            <PasswordPair />
          </div>
          <div className="rounded-lg bg-slate-100 p-3 text-sm text-slate-700"><TermsCheckbox /></div>
        </fieldset>
        <ProviderRegistrationFields company active={profile === "empresa"} />
        <ProviderRegistrationFields company={false} active={profile === "autonomo"} />
        <Feedback error={submittedProfile === profile ? error : ""} />
        <div className="flex justify-center">
          <button className={buttonStyle} disabled={busy}>{busy ? "Criando conta…" : "Concluir cadastro"}</button>
        </div>
      </fieldset>
    </form>
  </AuthCard>;
}

export function RecoveryPage() {
  const [remaining, setRemaining] = useState(0);
  const [sent, setSent] = useState(false);
  const { busy, error, handleSubmit } = useSubmit(async values => {
    if (remaining > 0) return;
    setSent(false);
    await authPost("recuperacao", values);
    setSent(true);
    setRemaining(60);
  });

  useEffect(() => {
    if (remaining === 0) return;
    const timeout = window.setTimeout(() => setRemaining(seconds => seconds - 1), 1000);
    return () => window.clearTimeout(timeout);
  }, [remaining]);

  return <AuthCard title="Recupere o acesso à sua conta"
    intro="Uma única conta para solicitar fretes e atuar como prestador autorizado." compact>
    <form className="space-y-3" onSubmit={handleSubmit}>
      <Field name="email" label="E-mail cadastrado" type="email" autoComplete="email" maxLength={255}
        readOnly={sent && remaining > 0} />
      {!sent && <button className={buttonStyle} disabled={busy}>
        {busy ? "Enviando…" : "Enviar link de recuperação"}
      </button>}
      <Feedback error={error} />
      {sent && <>
        <div role="status" className={successStyle}>
          <strong>Confira sua caixa de entrada</strong>
          <p>Se o e-mail estiver cadastrado, você receberá as instruções para criar uma nova senha.</p>
        </div>
        <p className="text-xs text-slate-600">
          Não recebeu? Confira a pasta de spam.
          {remaining > 0 && ` Tente novamente em ${remaining} s.`}
        </p>
      </>}
      <div className="flex flex-wrap gap-2">
        <Link to="/entrar" className={secondaryButtonStyle}>Voltar para entrar</Link>
        {sent && <button className={secondaryButtonStyle} disabled={busy || remaining > 0}>Reenviar link</button>}
      </div>
    </form>
  </AuthCard>;
}

export function ResetPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [token] = useState(() => new URLSearchParams(location.hash.slice(1)).get("token") ?? "");
  useEffect(() => {
    if (location.hash) navigate("/redefinir-senha", { replace: true });
  }, [location.hash, navigate]);
  const { busy, error, handleSubmit } = useSubmit(async values => {
    validatePasswords(values);
    await authPost("redefinicao", { ...values, token });
    navigate("/entrar", { replace: true, state: { notice: "Senha atualizada. Entre com sua nova senha." } });
  });
  return <AuthCard title="Crie uma nova senha" intro="Recuperação de acesso · Etapa final" compact>
    {token ? <form className="space-y-4" onSubmit={handleSubmit}>
      <PasswordPair reset />
      <Feedback error={error} />
      <button className={buttonStyle} disabled={busy}>{busy ? "Salvando…" : "Salvar senha e voltar para entrar"}</button>
    </form> : <p role="alert">Abra o link enviado por e-mail para redefinir sua senha.</p>}
    <p className="mb-2 mt-4 text-xs text-slate-600">Link expirado ou inválido? Solicite um novo link de recuperação.</p>
    <Link to="/recuperar-senha" className={`inline-block ${secondaryButtonStyle}`}>Solicitar novo link</Link>
  </AuthCard>;
}

export function ActivateIndependentForm({ onActivated }: { onActivated: () => void }) {
  const { busy, error, handleSubmit } = useSubmit(async values => {
    await authPost("perfil/autonomo", {
      cnh: values.cnh,
      categoriaCnh: values.categoriaCnh,
      declarouConformidade: values.declarouConformidade === "on",
    });
    onActivated();
  });
  return <form className="space-y-3 rounded-lg border border-slate-200 p-4" onSubmit={handleSubmit}>
    <h2 className="font-semibold">Atuar como prestador autônomo</h2>
    <p className="text-sm text-slate-600">Use a mesma conta de passageiro. A ativação do perfil não concede autorização para oferecer fretes.</p>
    <div className="grid gap-3 sm:grid-cols-2"><LicenseFields /></div>
    <div className="text-sm text-slate-700"><ComplianceCheckbox /></div>
    <Feedback error={error} />
    <button className={buttonStyle} disabled={busy}>{busy ? "Ativando…" : "Ativar perfil autônomo"}</button>
  </form>;
}

export function AccountPage() {
  const navigate = useNavigate();
  const [session, setSession] = useState<Session>();
  const [sessionError, setSessionError] = useState("");
  const [activated, setActivated] = useState(false);
  useEffect(() => {
    const abort = new AbortController();
    getSession(abort.signal).then(account => {
      if (account) setSession(account);
      else navigate("/entrar", { replace: true });
    }).catch(() => {
      if (!abort.signal.aborted) setSessionError("Não foi possível verificar sua sessão.");
    });
    return () => abort.abort();
  }, [navigate]);
  const { busy, error, handleSubmit } = useSubmit(async () => {
    await authPost("logout");
    navigate("/entrar", { replace: true });
  });
  return <AuthCard title="Sua conta MUV" intro={session ? `Olá, ${session.nome}.` : sessionError ? "" : "Verificando sessão…"}>
    <Feedback error={sessionError || error} />
    {session && <div className="space-y-4">
      <p>{session.email}</p>
      {activated && <p role="status" className={successStyle}>Perfil autônomo ativado na sua conta.</p>}
      {session.podeAtivarAutonomo && !activated && <ActivateIndependentForm onActivated={() => setActivated(true)} />}
      <form onSubmit={handleSubmit}>
        <button className={secondaryButtonStyle} disabled={busy}>Sair</button>
      </form>
    </div>}
  </AuthCard>;
}
