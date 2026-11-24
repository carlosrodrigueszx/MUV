interface AvatarProps {
  src?: string | null;
  alt?: string;
  name?: string;
  /** Tamanho em px. */
  size: number;
  className?: string;
}

/** Círculo com a foto; sem `src`, mostra o placeholder cinza do design. */
export function Avatar({ src, alt = "", size, name, className = "" }: AvatarProps) {
  const initials = name?.trim().split(/\s+/).slice(0, 2)
    .map(part => [...part][0]).join("").toLocaleUpperCase("pt-BR");
  return src ? (
    <img
      src={src}
      alt={alt}
      width={size}
      height={size}
      style={{ width: size, height: size }}
      className={`shrink-0 rounded-full object-cover ${className}`}
    />
  ) : (
    <span
      aria-hidden="true"
      style={{ width: size, height: size }}
      className={`grid shrink-0 place-items-center rounded-full ${initials
        ? "bg-[#e1f1eb] text-xs font-semibold text-[#005d50]" : "bg-[#d9d9d9]"} ${className}`}
    >{initials}</span>
  );
}
