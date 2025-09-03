#!/bin/bash

# Purge Docker images for Micrometer Analytics Services
# This script removes the Docker images created for the analytics services

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

FORCE=false
DRY_RUN=false

usage() {
  cat <<EOF
Usage: $0 [options]

Options:
  --force      Remove images without confirmation
  --dry-run    Show what would be removed without actually removing
  -h, --help   Show this help

This script removes Docker images:
  - user-analytics-service:latest
  - commerce-analytics-service:latest
  
Related tags and intermediate images are also cleaned up.
EOF
}

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    --force)
      FORCE=true; shift;;
    --dry-run)
      DRY_RUN=true; shift;;
    -h|--help)
      usage; exit 0;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"; usage; exit 1;;
  esac
done

echo -e "${BLUE}Purging Micrometer Analytics Docker images...${NC}"

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Docker could not be found. Please install Docker first.${NC}"
    exit 1
fi

# Function to remove image if it exists
remove_image() {
    local image_name=$1
    
    if docker image inspect "$image_name" &> /dev/null; then
        echo -e "${YELLOW}Found image: $image_name${NC}"
        if $DRY_RUN; then
            echo -e "${YELLOW}[dry-run] Would remove: docker image rm $image_name${NC}"
        else
            echo -e "${BLUE}Removing image: $image_name${NC}"
            docker image rm "$image_name" || echo -e "${YELLOW}Warning: Failed to remove $image_name${NC}"
        fi
    else
        echo -e "${YELLOW}Image not found: $image_name${NC}"
    fi
}

# List images that will be affected
echo -e "${BLUE}Checking for analytics service images...${NC}"

images_to_remove=(
    "user-analytics-service:latest"
    "commerce-analytics-service:latest"
)

found_images=()
for image in "${images_to_remove[@]}"; do
    if docker image inspect "$image" &> /dev/null; then
        found_images+=("$image")
    fi
done

if [ ${#found_images[@]} -eq 0 ]; then
    echo -e "${GREEN}No analytics service images found to remove.${NC}"
    exit 0
fi

echo -e "${YELLOW}Found ${#found_images[@]} image(s) to remove:${NC}"
for image in "${found_images[@]}"; do
    echo "  - $image"
done

# Confirm removal unless forced or dry run
if ! $FORCE && ! $DRY_RUN; then
    echo ""
    echo -e "${YELLOW}Are you sure you want to remove these images? [y/N]${NC}"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        echo -e "${BLUE}Aborted by user.${NC}"
        exit 0
    fi
fi

# Remove the images
for image in "${found_images[@]}"; do
    remove_image "$image"
done

# Clean up dangling images related to our build
if ! $DRY_RUN; then
    echo -e "${BLUE}Cleaning up dangling images...${NC}"
    dangling_count=$(docker image prune -f --filter "dangling=true" | grep "deleted" | wc -l || echo "0")
    if [ "$dangling_count" -gt 0 ]; then
        echo -e "${GREEN}Removed $dangling_count dangling image(s).${NC}"
    else
        echo -e "${YELLOW}No dangling images to clean up.${NC}"
    fi
fi

if $DRY_RUN; then
    echo -e "${GREEN}Dry run completed. Use --force to actually remove images.${NC}"
else
    echo -e "${GREEN}Docker image cleanup completed!${NC}"
fi

# Show remaining space
echo ""
echo -e "${BLUE}Current Docker disk usage:${NC}"
docker system df
