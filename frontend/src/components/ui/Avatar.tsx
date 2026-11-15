interface AvatarProps {
  src?: string | null;
  alt?: string;
  /** Tamanho em px. */
  size: number;
  className?: string;
}

/** Círculo com a foto; sem `src`, mostra o placeholder cinza do design. */
export function Avatar({ src, alt = "", size, className = "" }: AvatarProps) {
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
      className={`shrink-0 rounded-full bg-[#d9d9d9] ${className}`}
    />
  );
}
