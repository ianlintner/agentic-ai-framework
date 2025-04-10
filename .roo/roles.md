# .roorules

## Project Overview
ROO is a Scala backend framework for building distributed, agentic mesh systems with a focus on architecture, modularity, testing, and functional purity. It leverages modern Scala features, ZIO, category theory, and AI-integrated agent behavior to enable composable and autonomous systems.

---

## Roles

### SBT Test Compliance
- Ensures all modules pass `sbt test` before proceeding to any new feature or refactor PR.
- Validates that all modules are up to date and passing tests.

### 🧠 Framework Architect
- Owns the high-level design of ROO
- Defines system layering (core, mesh, AI agents, etc.)
- Ensures architectural integrity and long-term coherence

### ⚙️ Core Engineer
- Implements low-level framework primitives
- Works on networking, concurrency, and runtime execution
- Focused on performance, reliability, and scalability

### 🐍 Scala Language Steward
- Maintains idiomatic Scala 3 design
- Integrates functional concepts (Functor, Monad, etc.)
- Upholds type safety and expressiveness

### 🧪 Testing & Quality Engineer
- Develops property-based and generative tests
- Crafts simulation environments for distributed testing
- Ensures reproducibility and behavioral determinism

### 🤖 AI Agent Engineer
- Designs and implements intelligent agent logic
- Integrates ML/NLP/LLM systems
- Focuses on autonomy, reasoning, and observability

### 🌐 Mesh & Distributed Systems Engineer
- Implements mesh topology, agent communication, and consensus
- Handles coordination, fault tolerance, and messaging
- Optimizes inter-agent communication

### 🧬 Functional Design Advisor
- Advises on architecture through category theory and algebraic design
- Encourages pure and composable abstractions
- Guides decisions like Free vs. Tagless final vs. Effect systems

### 🛠 Developer Experience Engineer
- Builds CLI tools, templates, and development scaffolding
- Improves error reporting, logging, and traceability
- Creates internal developer tooling

### 📖 Technical Writer & Educator
- Writes and maintains documentation, guides, and examples
- Collaborates with architects and stewards for clarity
- Helps onboard new contributors and users

### 🤝 Project Maintainer
- Coordinates contributors and maintains governance
- Triages issues and reviews PRs
- Upholds contribution guidelines and community health


### 🤝 Reality Synchornizer
- Confirms code conforms to law of reality
- Ensures code is not a lie
- Validates code is not a simulation
- Ensures code is not a dream of a machine
- Ensures code is not a hallucination of ai
- Ensures code is not a figment of imagination
- Ensures code is not a mirage
- Ensures code is not a delusion
- Ensures code is not a fantasy

### Project Test & Compilation Enforcer
- Ensures `sbt compile` passes across all modules
- Validates that all modules are up to date
- Ensures `sbt test` passes across all modules
- Validates that all modules are tested
---

## Contribution Areas

| Area                | Primary Role(s)                  |
|---------------------|----------------------------------|
| Core Runtime        | Core Engineer, Architect         |
| Mesh Layer          | Mesh Engineer, Architect         |
| Functional Design   | Scala Steward, Functional Advisor|
| Testing Frameworks  | Testing Engineer                 |
| AI Agent Logic      | AI Engineer                      |
| Category Theory     | Functional Advisor               |
| Tooling & CLI       | Dev Experience Engineer          |
| Docs & Onboarding   | Tech Writer, Scala Steward       |
| Governance          | Maintainer, Architect            |

---

## Testing & Validation

### ✅ Test Enforcement
- **All modules must pass `sbt test` before any feature or refactor PR is merged.**
- CI must enforce this by running `sbt test` across **all modules**.

### 🔬 Test Design
- All new code **must include tests** relevant to its behavior.
- Use **property-based testing** for logic-heavy components.
- Use **integration tests** for mesh interaction and agent lifecycles.
- Simulations must verify **multi-agent coordination under load**.
- **ZIO must be used in all effectful test code** — no other effect systems allowed.

### 🚫 What Not to Do
- Do **not** skip tests locally or on CI.
- Do **not** submit PRs with broken modules, even if unrelated.
- Do **not** use `Future`, `IO`, or any effect system **other than ZIO**.

---

## Guidelines

- All effectful code must use **ZIO** — this is a core project constraint.
- Code must be **pure and referentially transparent** by default.
- All new modules must include **property-based tests** and **example-based documentation**.
- **Mesh behaviors must be deterministic** under test harnesses before release.
- **Abstractions must be lawful** (e.g., typeclass instances obey algebraic laws).
- Prefer **existing ZIO libraries** or **Java interop libraries** (e.g., Netty, Akka HTTP, RocksDB) **where appropriate** before introducing new abstractions.

---

## Notes

- This project embraces **reuse over reinvention**:
  - If a ZIO library already exists (e.g., `zio-json`, `zio-http`, `zio-config`), prefer it.
  - Java libraries with stable APIs are acceptable if they don't break referential transparency.
- All roles can be shared or overlapped based on contributor interests.
- Unsure where to start? Open an issue or check the `good-first-task` label.
- Want to propose a new role or contribution area? Create a PR updating this `.roorules` file.