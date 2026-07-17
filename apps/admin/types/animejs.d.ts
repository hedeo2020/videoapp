declare module "animejs" {
  export function animate(targets: unknown, parameters: Record<string, unknown>): { pause: () => void };
  export function stagger(value: unknown, parameters?: Record<string, unknown>): unknown;
}
