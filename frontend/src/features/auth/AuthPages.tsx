import { useEffect, useState, type ComponentProps, type FormEvent, type ReactNode } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { LuBus, LuEye, LuEyeOff } from "react-icons/lu";
import { authPost, getSession, type Session } from "./api";

const inputStyle = "w-full rounded-lg border border-slate-200 bg-slate-100 px-3 py-2.5 text-slate-900 focus-visible:outline-2 focus-visible:outline-[#1A5344]";
const buttonStyle = "rounded-lg bg-[#1A5344] px-4 py-2.5 font-semibold text-white hover:bg-[#144236] disabled:opacity-60";
const linkStyle = "font-semibold text-[#1A5344] underline underline-offset-2";

function Field({ label, secret, ...props }: ComponentProps<"input"> & { label: string; secret?: boolean }) {
  const [visible, setVisible] = useState(false);
  return <label className="block space-y-1 text-sm font-medium text-slate-700">
    <span>{label}</span>
    <span className="relative block">
      <input {...props} type={secret ? visible ? "text" : "password" : props.type}
        className={`${inputStyle} ${secret ? "pr-11" : ""}`} required />
      {secret && <button type="button" aria-label={visible ? "Ocultar senha" : "Mostrar senha"}
        className="absolute inset-y-0 right-0 px-3 text-slate-600" onClick={() => setVisible(!visible)}>
        {visible ? <LuEyeOff /> : <LuEye />}
      </button>}
    </span>
  </label>;
}

function AuthCard({ title, intro, wide, children }: { title: string; intro: string; wide?: boolean; children: ReactNode }) {
  return <main className="grid min-h-screen place-items-center bg-slate-100 px-4 py-10 text-slate-900">
    <section className={`w-full rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8 ${wide ? "max-w-2xl" : "max-w-md"}`}>
      <Link className={linkStyle} to="/">← Voltar ao início</Link>
      <div className="mt-6 mb-6">
        <span className="mb-3 inline-grid size-10 place-items-center rounded-xl bg-[#1A5344] text-white"><LuBus aria-hidden="true" /></span>
        <h1 className="text-2xl font-bold text-[#1A5344]">{title}</h1>
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

function PasswordPair() {
  return <>
    <Field name="senha" label="Senha de acesso" secret autoComplete="new-password" minLength={8} maxLength={72} />
    <Field name="confirmarSenha" label="Confirmar senha" secret autoComplete="new-password" minLength={8} maxLength={72} />
  </>;
}

function validateMatch(values: Record<string, string>) {
  if (values.senha !== values.confirmarSenha) throw new Error("As senhas não coincidem.");
}

export function LoginPage() {
  const navigate = useNavigate();
  const notice = useLocation().state?.notice as string | undefined;
  const { busy, error, handleSubmit } = useSubmit(async values => {
    await authPost("login", new URLSearchParams(values));
    navigate("/conta", { replace: true });
  });

  return <AuthCard title="Portal de Acesso MUV" intro="Entre para acessar sua conta.">
    {notice && <p role="status" className="mb-4 text-sm text-[#1A5344]">{notice}</p>}
    <form className="space-y-4" onSubmit={handleSubmit}>
      <Field name="email" label="E-mail" type="email" autoComplete="username" maxLength={255} />
      <Field name="senha" label="Senha" secret autoComplete="current-password" maxLength={72} />
      <Link to="/recuperar-senha" className={`block text-right text-sm ${linkStyle}`}>Esqueceu a senha?</Link>
      <Feedback error={error} />
      <button className={`w-full ${buttonStyle}`} disabled={busy}>{busy ? "Entrando…" : "Entrar"}</button>
    </form>
    <p className="mt-5 rounded-lg bg-slate-100 p-3 text-sm">Ainda não tem conta? <Link to="/cadastro" className={linkStyle}>Criar nova conta</Link></p>
  </AuthCard>;
}

export function RegisterPage() {
  const navigate = useNavigate();
  const { busy, error, handleSubmit } = useSubmit(async values => {
    validateMatch(values);
    await authPost("cadastro", values);
    navigate("/entrar", { replace: true, state: { notice: "Conta criada. Entre com seu e-mail e senha." } });
  });

  return <AuthCard title="Crie sua Conta no MUV" intro="Cadastre-se como passageiro para usar sua conta no MUV." wide>
    <p className="mb-4 rounded-lg bg-[#1A5344] p-3 font-semibold text-white">Passageiro</p>
    <form className="space-y-4" onSubmit={handleSubmit}>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field name="nome" label="Nome completo" autoComplete="name" maxLength={255} />
        <Field name="nascimento" label="Data de nascimento" type="date" autoComplete="bday" />
        <Field name="cpf" label="CPF do titular" inputMode="numeric" placeholder="000.000.000-00" maxLength={14} />
        <label className="block space-y-1 text-sm font-medium text-slate-700">
          <span>Gênero / identidade</span>
          <select name="genero" className={inputStyle} defaultValue="" required>
            <option value="" disabled>Selecione</option>
            <option>Feminino</option><option>Masculino</option><option>Não binário</option>
            <option>Outro</option><option>Prefiro não informar</option>
          </select>
        </label>
        <Field name="email" label="E-mail principal" type="email" autoComplete="email" maxLength={255} />
        <Field name="telefone" label="Celular" type="tel" autoComplete="tel" maxLength={20} />
        <PasswordPair />
      </div>
      <p className="text-sm text-slate-600">Use uma senha com pelo menos 8 caracteres.</p>
      <Feedback error={error} />
      <button className={buttonStyle} disabled={busy}>{busy ? "Criando conta…" : "Concluir cadastro"}</button>
    </form>
    <p className="mt-5 text-sm">Já tem conta? <Link to="/entrar" className={linkStyle}>Fazer login</Link></p>
  </AuthCard>;
}

export function RecoveryPage() {
  const [sent, setSent] = useState(false);
  const { busy, error, handleSubmit } = useSubmit(async values => {
    await authPost("recuperacao", values);
    setSent(true);
  });
  return <AuthCard title="Recuperar senha" intro="Informe o e-mail associado à sua conta.">
    {sent ? <p role="status" className="text-sm">Se o e-mail estiver cadastrado, você receberá um link para redefinir a senha.</p>
      : <form className="space-y-4" onSubmit={handleSubmit}>
        <Field name="email" label="E-mail" type="email" autoComplete="email" maxLength={255} />
        <Feedback error={error} />
        <button className={buttonStyle} disabled={busy}>{busy ? "Enviando…" : "Enviar link"}</button>
      </form>}
    <Link to="/entrar" className={`mt-5 inline-block ${linkStyle}`}>Voltar ao login</Link>
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
    validateMatch(values);
    await authPost("redefinicao", { ...values, token });
    navigate("/entrar", { replace: true, state: { notice: "Senha atualizada. Entre com sua nova senha." } });
  });
  return <AuthCard title="Redefinir senha" intro="Escolha uma nova senha para sua conta.">
    {token ? <form className="space-y-4" onSubmit={handleSubmit}>
      <PasswordPair />
      <Feedback error={error} />
      <button className={buttonStyle} disabled={busy}>{busy ? "Salvando…" : "Salvar nova senha"}</button>
    </form> : <p role="alert">Abra o link enviado por e-mail para redefinir sua senha.</p>}
    <Link to="/recuperar-senha" className={`mt-5 inline-block ${linkStyle}`}>Solicitar outro link</Link>
  </AuthCard>;
}

export function AccountPage() {
  const navigate = useNavigate();
  const [session, setSession] = useState<Session>();
  const [sessionError, setSessionError] = useState("");
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
    {session && <form className="space-y-4" onSubmit={handleSubmit}>
      <p>{session.email}</p>
      <button className={buttonStyle} disabled={busy}>Sair</button>
    </form>}
  </AuthCard>;
}
