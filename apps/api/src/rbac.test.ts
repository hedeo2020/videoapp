import assert from "node:assert/strict";
import test from "node:test";
import { hasPermission } from "./rbac.js";
test("server permissions keep editors away from sensitive settings",()=>{assert.equal(hasPermission("EDITOR","catalog:write"),true);assert.equal(hasPermission("EDITOR","settings:security"),false)});
test("only super admins manage administrators",()=>{assert.equal(hasPermission("SUPER_ADMIN","admin:manage"),true);assert.equal(hasPermission("ADMIN","admin:manage"),false)});
