#!/bin/bash

# Example: Adding a New Prometheus Recording Rule
# This script demonstrates how to add a new rule and update Prometheus

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=======================================${NC}"
echo -e "${BLUE} Example: Adding a New Recording Rule${NC}"
echo -e "${BLUE}=======================================${NC}"
echo ""

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
K8S_DIR="$(dirname "$SCRIPT_DIR")"
RECORDING_RULES_FILE="$K8S_DIR/prometheus/recording-rules.yaml"

echo -e "${YELLOW}This example shows how to add a new recording rule:${NC}"
echo "  commerce:error_rate - Error rate for commerce service"
echo ""

# Check if rules file exists
if [[ ! -f "$RECORDING_RULES_FILE" ]]; then
    echo -e "${RED}Recording rules file not found: $RECORDING_RULES_FILE${NC}"
    exit 1
fi

echo -e "${YELLOW}Step 1: Current rule content preview${NC}"
echo -e "${BLUE}Current groups in recording rules:${NC}"
grep "^  - name:" "$RECORDING_RULES_FILE" || echo "No rule groups found"
echo ""

echo -e "${YELLOW}Step 2: Adding new rule to commerce_analytics_rates group${NC}"

# Create a backup first
BACKUP_FILE="${RECORDING_RULES_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
cp "$RECORDING_RULES_FILE" "$BACKUP_FILE"
echo -e "${GREEN}✓ Created backup: $(basename "$BACKUP_FILE")${NC}"

# The new rule we want to add
NEW_RULE='      
      # Commerce service error rate
      - record: commerce:error_rate
        expr: |
          sum(rate(http_server_requests_total{service="commerce-analytics",status=~"5.."}[5m])) /
          sum(rate(http_server_requests_total{service="commerce-analytics"}[5m])) * 100'

# Check if rule already exists
if grep -q "commerce:error_rate" "$RECORDING_RULES_FILE"; then
    echo -e "${YELLOW}⚠ Rule 'commerce:error_rate' already exists, skipping addition${NC}"
else
    echo -e "${BLUE}Adding new rule...${NC}"
    
    # Find the commerce_analytics_rates group and add the rule before the next group
    # This is a simple approach - in practice you might want more robust YAML editing
    if grep -q "name: commerce_analytics_rates" "$RECORDING_RULES_FILE"; then
        # Find the line number of the next group after commerce_analytics_rates
        local next_group_line=$(grep -n "^    - name:" "$RECORDING_RULES_FILE" | grep -A1 "commerce_analytics_rates" | tail -1 | cut -d: -f1)
        
        if [[ -n "$next_group_line" ]]; then
            # Insert before the next group
            sed -i.tmp "${next_group_line}i\\
$NEW_RULE
" "$RECORDING_RULES_FILE"
        else
            # Add at the end of the file (before the last group or at the very end)
            echo "$NEW_RULE" >> "$RECORDING_RULES_FILE"
        fi
        echo -e "${GREEN}✓ Added commerce:error_rate rule${NC}"
    else
        echo -e "${RED}✗ commerce_analytics_rates group not found${NC}"
        echo "Available groups:"
        grep "^  - name:" "$RECORDING_RULES_FILE"
        exit 1
    fi
fi

echo ""
echo -e "${YELLOW}Step 3: Validating and applying the updated rules${NC}"

# Use our update script to validate and apply
if "$SCRIPT_DIR/update-rules.sh" --validate-only; then
    echo -e "${GREEN}✓ Rule validation passed${NC}"
    echo ""
    
    echo -e "${YELLOW}Step 4: Applying the updated rules${NC}"
    "$SCRIPT_DIR/update-rules.sh" --wait
    
    echo ""
    echo -e "${GREEN}✓ Rule update completed!${NC}"
    echo ""
    
    echo -e "${BLUE}Step 5: Testing the new rule${NC}"
    echo "You can now test the new rule with:"
    echo "  1. Access Prometheus UI: kubectl port-forward -n micrometer-analytics svc/prometheus 9090:9090"
    echo "  2. Query the new rule: commerce:error_rate"
    echo "  3. Or via API: curl 'http://localhost:9090/api/v1/query?query=commerce:error_rate'"
    echo ""
    
    echo -e "${YELLOW}Example PromQL queries using the new rule:${NC}"
    echo "  commerce:error_rate                                    # Current error rate"
    echo "  avg_over_time(commerce:error_rate[1h])                # 1-hour average error rate"  
    echo "  commerce:error_rate > 5                               # Alert condition (error rate > 5%)"
    
else
    echo -e "${RED}✗ Rule validation failed${NC}"
    echo -e "${YELLOW}Restoring backup...${NC}"
    mv "$BACKUP_FILE" "$RECORDING_RULES_FILE"
    echo -e "${GREEN}✓ Backup restored${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}=======================================${NC}"
echo -e "${BLUE} Example completed successfully!${NC}"
echo -e "${BLUE}=======================================${NC}"
echo ""
echo -e "${YELLOW}What we did:${NC}"
echo "1. ✓ Created a backup of the rules file"
echo "2. ✓ Added a new recording rule: commerce:error_rate"
echo "3. ✓ Validated the rule syntax"
echo "4. ✓ Applied the updated ConfigMap to Kubernetes"
echo "5. ✓ Triggered Prometheus to reload its configuration"
echo "6. ✓ Verified the rules were loaded"
echo ""
echo -e "${YELLOW}Cleanup:${NC}"
echo "The backup file is saved as: $(basename "$BACKUP_FILE")"
echo "You can remove it with: rm $BACKUP_FILE"
