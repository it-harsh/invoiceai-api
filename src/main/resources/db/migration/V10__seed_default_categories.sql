-- Function to seed default categories when a new organization is created.
-- Called from application code (OrganizationService) after org creation.
-- This migration just documents the default set.

-- Default categories:
-- Office Supplies     #3B82F6  folder
-- Travel              #10B981  plane
-- Software            #8B5CF6  monitor
-- Meals               #F59E0B  utensils
-- Professional Svcs   #EF4444  briefcase
-- Utilities           #6366F1  zap
-- Marketing           #EC4899  megaphone
-- Other               #6B7280  circle

-- No-op migration — seeding is done in application code.
-- This file exists to document the convention and reserve the version number.
SELECT 1;
