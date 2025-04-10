# Test Reports for Agentic AI Framework

This document explains how to run tests and generate detailed reports for the Agentic AI Framework project, both using GitHub Actions workflows and a local shell script.

## Local Test Script (Recommended)

For quick local testing and reporting, we provide a shell script that can be run directly from your development environment without requiring GitHub Actions.

### Using the Local Test Script

The `run-tests-with-reports.sh` script in the root directory provides a convenient way to run tests and generate reports:

```bash
# Run tests for all modules
./run-tests-with-reports.sh --all

# Run tests for specific modules
./run-tests-with-reports.sh --modules=core,mesh

# Skip coverage reports for faster execution
./run-tests-with-reports.sh --modules=core --skip-coverage
```

The script will:
1. Run the tests for the specified modules
2. Generate HTML test reports
3. Create coverage reports (unless skipped)
4. Provide module-specific test and coverage summaries
5. Save all reports to a timestamped directory

To see all available options, run:
```bash
./run-tests-with-reports.sh --help
```

## GitHub Actions Workflows

The project includes two main testing workflows:

1. **Scala CI** (`scala.yml`) - This is the standard CI workflow that runs on all pull requests and pushes to main branches.
2. **Scala Tests with Reports** (`scala-test-reports.yml`) - This is a more detailed workflow that generates comprehensive test and coverage reports.

## Running Tests Locally Using GitHub Actions

The `scala-test-reports.yml` workflow is designed to be run locally using GitHub CLI, making it easy to generate the same reports on your development machine without pushing to remote. This is particularly useful for:

- Validating test coverage before submitting a PR
- Generating detailed HTML test reports
- Testing specific modules

### Prerequisites

1. Install [GitHub CLI](https://cli.github.com/) on your machine
2. Login to GitHub CLI using `gh auth login`

### Running the Test Reports Workflow

#### Basic Usage

To run all tests with coverage reports:

```bash
gh workflow run scala-test-reports.yml
```

This will:
- Run all tests across all modules
- Generate code coverage reports
- Create HTML test reports
- Create module-specific coverage summaries

#### Testing Specific Modules

You can choose to test only specific modules by providing a comma-separated list:

```bash
gh workflow run scala-test-reports.yml -f modules=core,mesh
```

This will run tests only for the core and mesh modules.

#### Skipping Coverage Reports

If you want faster test execution and don't need coverage reports:

```bash
gh workflow run scala-test-reports.yml -f skip-coverage=true
```

### Viewing Report Artifacts

After the workflow completes, you can download the report artifacts:

```bash
# List workflow runs
gh run list --workflow=scala-test-reports.yml

# Download artifacts from a specific run (replace RUN_ID with the actual run ID)
gh run download RUN_ID
```

The available artifacts include:
- `test-reports-xml` - XML test reports
- `test-reports-html` - HTML test reports
- `coverage-reports` - Detailed coverage reports
- `test-summary` - Markdown summary of test results
- `coverage-summary` - Markdown summary of module-specific coverage

## Understanding the Reports

### Test Reports

The HTML test reports provide detailed information about:
- Test pass/fail status
- Test execution time
- Error messages and stack traces for failed tests
- Test hierarchies and suites

### Coverage Reports

The coverage reports show:
- Statement coverage (percentage of code statements executed)
- Branch coverage (percentage of conditionals executed)
- Coverage breakdown by package, class, and method
- Highlighted source code showing covered and uncovered lines

## Integrating with Codecov

The workflow automatically uploads coverage data to Codecov if it's configured for your repository, providing:
- Historical coverage tracking
- Pull request coverage comments
- Coverage visualizations

## Tips for Effective Testing

1. **Run module-specific tests during development**: When working on a specific module, use the `modules` parameter to only test that module for faster feedback.
2. **Skip coverage for quick tests**: Use `skip-coverage=true` when you just want to verify tests pass without generating full coverage reports.
3. **Review HTML reports for test failures**: When tests fail, the HTML reports provide much more detailed information than console output.
4. **Check coverage before submitting PRs**: Run the full workflow with coverage before submitting PRs to ensure you maintain or improve code coverage.

## Troubleshooting

If you encounter issues with the workflow:

1. **Workflow times out**: For large codebases, try testing specific modules instead of all modules.
2. **Coverage reports show 0%**: Ensure your tests actually exercise the code. ZIO tests should be run with the appropriate test aspect.
3. **XML parsing errors**: These typically indicate test failures with unusual characters in the output. Check the test logs for details.