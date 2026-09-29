"use client"

import {
  motion,
  useMotionValue,
  useSpring,
  useTransform,
} from "framer-motion";
import {
  KeyRound,
  GitBranch,
  Users,
  Lock,
  CheckCircle2,
  UserCheck,
} from "lucide-react";

/* ---------- SETTINGS: colors and content ---------- */

const RED = "#DC2626";
const NAVY = "#1E4E8C";

// x and y are percentages of the orbit circle's box:
// (50, 0) = top-middle, (100, 50) = right-middle, (50, 100) = bottom-middle
const outerNodes = [
  {
    x: 50,
    y: 0,
    icon: <span className="text-lg font-bold" style={{ color: "#4285F4" }}>G</span>,
  },
  { x: 93.3, y: 75, icon: <GitBranch size={20} className="text-slate-800" /> },
  { x: 6.7, y: 75, icon: <KeyRound size={20} style={{ color: NAVY }} /> },
];

const innerNodes = [
  { x: 85.4, y: 85.4, icon: <Users size={20} style={{ color: RED }} /> },
  { x: 14.6, y: 14.6, icon: <Lock size={20} style={{ color: RED }} /> },
];

const chips = [
  {
    label: "Identity verified",
    icon: <CheckCircle2 size={16} style={{ color: NAVY }} />,
    position: "right-0 top-[12%]",
    delay: 0,
  },
  {
    label: "Role: Admin",
    icon: <UserCheck size={16} style={{ color: RED }} />,
    position: "bottom-[16%] left-0",
    delay: 1.3,
  },
  {
    label: "Session secured",
    icon: <Lock size={16} style={{ color: NAVY }} />,
    position: "bottom-[4%] right-[4%]",
    delay: 2.4,
  },
];

/* ---------- Orbit: a spinning dashed circle carrying icons ---------- */

function Orbit({ inset, duration, reverse = false, color, nodes }) {
  const turn = reverse ? -360 : 360;

  return (
    <motion.div
      className="absolute rounded-full border-[1.5px] border-dashed"
      style={{ inset, borderColor: color }}
      animate={{ rotate: turn }}
      transition={{ duration, repeat: Infinity, ease: "linear" }}
    >
      {nodes.map((node, i) => (
        <div
          key={i}
          className="absolute -translate-x-1/2 -translate-y-1/2"
          style={{ left: `${node.x}%`, top: `${node.y}%` }}
        >
          {/* spin the icon the opposite way so it stays upright */}
          <motion.div
            className="flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white shadow-md"
            animate={{ rotate: -turn }}
            transition={{ duration, repeat: Infinity, ease: "linear" }}
          >
            {node.icon}
          </motion.div>
        </div>
      ))}
    </motion.div>
  );
}

/* ---------- FloatingChip: a glass label that bobs up and down ---------- */

function FloatingChip({ label, icon, position, delay }) {
  return (
    <motion.div
      className={`absolute flex items-center gap-2 whitespace-nowrap rounded-xl border border-slate-200 bg-white/80 px-3 py-2 text-xs font-medium text-[#0F1B3D] shadow-lg backdrop-blur ${position}`}
      style={{ z: 60 }}
      animate={{ y: [0, -10, 0] }}
      transition={{ duration: 4, repeat: Infinity, ease: "easeInOut", delay }}
    >
      {icon}
      {label}
    </motion.div>
  );
}

/* ---------- Main component ---------- */

export default function SecureOrbit() {
  // --- optional mouse tilt: delete this block to remove it ---
  const mx = useMotionValue(0);
  const my = useMotionValue(0);
  const spring = { stiffness: 120, damping: 16 };
  const rotateY = useSpring(useTransform(mx, [-0.5, 0.5], [-14, 14]), spring);
  const rotateX = useSpring(useTransform(my, [-0.5, 0.5], [12, -12]), spring);

  const handleMove = (e) => {
    const box = e.currentTarget.getBoundingClientRect();
    mx.set((e.clientX - box.left) / box.width - 0.5);
    my.set((e.clientY - box.top) / box.height - 0.5);
  };
  const handleLeave = () => {
    mx.set(0);
    my.set(0);
  };
  // -----------------------------------------------------------

  return (
    <div
      className="relative mx-auto aspect-square w-full max-w-[520px]"
      style={{ perspective: 1000 }}
      onMouseMove={handleMove}
      onMouseLeave={handleLeave}
    >
      <motion.div
        className="absolute inset-0"
        style={{ rotateX, rotateY, transformStyle: "preserve-3d" }}
      >
        {/* 1. soft glow */}
        <div
          className="absolute inset-[10%] rounded-full opacity-80"
          style={{
            background:
              "radial-gradient(circle, #FEE2E2 0%, #DBEAFE 60%, rgba(248,250,252,0) 100%)",
          }}
        />

        {/* 2. scanner beam */}
        <motion.div
          className="absolute inset-[4%] rounded-full"
          style={{
            background:
              "conic-gradient(from 0deg, rgba(220,38,38,0) 0deg, rgba(220,38,38,0.22) 70deg, rgba(220,38,38,0) 72deg)",
          }}
          animate={{ rotate: 360 }}
          transition={{ duration: 5, repeat: Infinity, ease: "linear" }}
        />

        {/* 3. orbits, spinning in opposite directions */}
        <Orbit inset="4%" duration={28} color="#FCA5A5" nodes={outerNodes} />
        <Orbit inset="20%" duration={20} reverse color="#93C5FD" nodes={innerNodes} />

        {/* 4. ripples from the center */}
        {[0, 1.5].map((delay) => (
          <motion.div
            key={delay}
            className="absolute inset-[36%] rounded-full border-2"
            style={{ borderColor: RED }}
            initial={{ scale: 1, opacity: 0.5 }}
            animate={{ scale: 2.4, opacity: 0 }}
            transition={{ duration: 3, repeat: Infinity, ease: "easeOut", delay }}
          />
        ))}

        {/* 5. core: shield with a self-drawing check mark */}
        <div
          className="absolute inset-[35%] flex items-center justify-center rounded-full bg-white shadow-2xl"
          style={{ border: `3px solid ${RED}`, transform: "translateZ(30px)" }}
        >
          <svg
            viewBox="0 0 24 24"
            className="h-1/2 w-1/2"
            fill="none"
            stroke={RED}
            strokeWidth={1.6}
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M12 2.5 20 5.5v6c0 5-3.4 8.6-8 10-4.6-1.4-8-5-8-10v-6l8-3z" />
            <motion.path
              d="M8.5 12.2l2.5 2.5 4.5-5"
              initial={{ pathLength: 0 }}
              animate={{ pathLength: [0, 1, 1, 0] }}
              transition={{
                duration: 3.2,
                repeat: Infinity,
                times: [0, 0.35, 0.85, 1],
                ease: "easeInOut",
              }}
            />
          </svg>
        </div>

        {/* 6. floating cards */}
        {chips.map((chip) => (
          <FloatingChip key={chip.label} {...chip} />
        ))}
      </motion.div>
    </div>
  );
}