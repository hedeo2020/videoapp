import { stdin, stdout } from "node:process";
import { createInterface } from "node:readline/promises";
import { PrismaClient } from "@prisma/client";
import argon2 from "argon2";

const prisma = new PrismaClient();
const terminal = createInterface({ input: stdin, output: stdout });
try {
  const email = (process.env.BOOTSTRAP_ADMIN_EMAIL ?? (await terminal.question("Administrator email: ")))
    .trim()
    .toLowerCase();
  const displayName = (process.env.BOOTSTRAP_ADMIN_NAME ?? (await terminal.question("Display name: "))).trim();
  const password = process.env.BOOTSTRAP_ADMIN_PASSWORD ?? (await terminal.question("Password (12+ characters): "));
  if (password.length < 12) throw new Error("Password must contain at least 12 characters");
  const count = await prisma.user.count({ where: { role: { in: ["SUPER_ADMIN", "ADMIN"] } } });
  if (count > 0 && !process.env.ALLOW_ADDITIONAL_SUPER_ADMIN)
    throw new Error("An administrator already exists. Use the audited admin workflow for additional accounts.");
  const user = await prisma.user.create({
    data: {
      email,
      displayName,
      passwordHash: await argon2.hash(password, { type: argon2.argon2id }),
      role: "SUPER_ADMIN",
      emailVerifiedAt: new Date(),
    },
  });
  console.log(
    `Created SUPER_ADMIN ${user.email}. Remove bootstrap secrets and rotate automated credentials immediately.`,
  );
} finally {
  terminal.close();
  await prisma.$disconnect();
}
