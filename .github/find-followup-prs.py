#!/usr/bin/env python3
"""
Find follow-up pull requests that should be backported along with a given PR.

This script analyzes a source PR and identifies subsequent PRs that:
1. Modified the same files
2. Reference the same issues
3. Fix bugs introduced by the source PR
4. Are related based on commit message analysis

Usage:
    python find-followup-prs.py --pr 50799 [--repo quarkusio/quarkus] [--github-token TOKEN]
"""

import argparse
import os
import sys
import json
from datetime import datetime
from typing import Dict, List, Set, Optional, Tuple
from collections import defaultdict
import re

try:
    import requests
except ImportError:
    print("Error: 'requests' module is required. Install it with: pip install requests")
    sys.exit(1)


class GitHubAPIClient:
    """Client for GitHub API interactions."""
    
    def __init__(self, token: Optional[str] = None):
        self.token = token or os.environ.get('GITHUB_TOKEN')
        self.base_url = "https://api.github.com"
        self.session = requests.Session()
        if self.token:
            self.session.headers.update({'Authorization': f'token {self.token}'})
        self.session.headers.update({'Accept': 'application/vnd.github.v3+json'})
    
    def get_pr(self, repo: str, pr_number: int) -> Dict:
        """Get PR details."""
        url = f"{self.base_url}/repos/{repo}/pulls/{pr_number}"
        response = self.session.get(url)
        response.raise_for_status()
        return response.json()
    
    def get_pr_files(self, repo: str, pr_number: int) -> List[Dict]:
        """Get files changed in a PR."""
        url = f"{self.base_url}/repos/{repo}/pulls/{pr_number}/files"
        files = []
        page = 1
        while True:
            response = self.session.get(url, params={'page': page, 'per_page': 100})
            response.raise_for_status()
            batch = response.json()
            if not batch:
                break
            files.extend(batch)
            page += 1
        return files
    
    def get_pr_commits(self, repo: str, pr_number: int) -> List[Dict]:
        """Get commits in a PR."""
        url = f"{self.base_url}/repos/{repo}/pulls/{pr_number}/commits"
        commits = []
        page = 1
        while True:
            response = self.session.get(url, params={'page': page, 'per_page': 100})
            response.raise_for_status()
            batch = response.json()
            if not batch:
                break
            commits.extend(batch)
            page += 1
        return commits
    
    def search_prs(self, repo: str, query: str, sort: str = 'created', 
                   order: str = 'asc') -> List[Dict]:
        """Search for PRs using GitHub search API."""
        url = f"{self.base_url}/search/issues"
        all_results = []
        page = 1
        
        while True:
            params = {
                'q': f'repo:{repo} is:pr {query}',
                'sort': sort,
                'order': order,
                'page': page,
                'per_page': 100
            }
            response = self.session.get(url, params=params)
            response.raise_for_status()
            data = response.json()
            
            if not data.get('items'):
                break
                
            all_results.extend(data['items'])
            
            # Check if we've got all results
            if len(all_results) >= data.get('total_count', 0):
                break
            
            page += 1
            
            # GitHub search API has a limit
            if page > 10:  # Max 1000 results
                break
        
        return all_results


class PRAnalyzer:
    """Analyzes PRs to find follow-ups."""
    
    def __init__(self, client: GitHubAPIClient, repo: str):
        self.client = client
        self.repo = repo
    
    def extract_issue_refs(self, text: str) -> Set[int]:
        """Extract issue/PR references from text."""
        if not text:
            return set()
        
        # Patterns: #1234, GH-1234, fixes #1234, closes #1234, etc.
        patterns = [
            r'#(\d+)',
            r'GH-(\d+)',
            r'(?:fix|fixes|fixed|close|closes|closed|resolve|resolves|resolved)\s+#(\d+)',
        ]
        
        refs = set()
        for pattern in patterns:
            matches = re.findall(pattern, text, re.IGNORECASE)
            refs.update(int(m) for m in matches)
        
        return refs
    
    def get_pr_context(self, pr_number: int) -> Dict:
        """Get comprehensive context about a PR."""
        pr = self.client.get_pr(self.repo, pr_number)
        files = self.client.get_pr_files(self.repo, pr_number)
        commits = self.client.get_pr_commits(self.repo, pr_number)
        
        # Extract file paths
        file_paths = {f['filename'] for f in files}
        
        # Extract issue references
        issue_refs = set()
        issue_refs.update(self.extract_issue_refs(pr.get('title', '')))
        issue_refs.update(self.extract_issue_refs(pr.get('body', '')))
        for commit in commits:
            issue_refs.update(self.extract_issue_refs(commit['commit']['message']))
        
        # Get merge date
        merged_at = pr.get('merged_at')
        if merged_at:
            merged_at = datetime.strptime(merged_at, '%Y-%m-%dT%H:%M:%SZ')
        
        return {
            'pr': pr,
            'number': pr_number,
            'files': file_paths,
            'issue_refs': issue_refs,
            'merged_at': merged_at,
            'title': pr.get('title', ''),
            'body': pr.get('body', ''),
            'commits': commits,
            'labels': {label['name'] for label in pr.get('labels', [])}
        }
    
    def find_followups(self, source_pr_number: int, max_results: int = 50) -> List[Tuple[int, float, Dict]]:
        """Find follow-up PRs with relevance scores."""
        print(f"Analyzing source PR #{source_pr_number}...")
        source = self.get_pr_context(source_pr_number)
        
        if not source['merged_at']:
            print(f"Warning: PR #{source_pr_number} is not merged yet.")
            return []
        
        print(f"Source PR merged on: {source['merged_at']}")
        print(f"Files changed: {len(source['files'])}")
        print(f"Issue references: {source['issue_refs']}")
        
        # Search for PRs merged after the source PR
        merged_after = source['merged_at'].strftime('%Y-%m-%d')
        query = f'is:merged merged:>={merged_after}'
        
        print(f"\nSearching for PRs merged after {merged_after}...")
        candidate_prs = self.client.search_prs(self.repo, query, sort='created', order='asc')
        
        # Filter out the source PR itself
        candidate_prs = [pr for pr in candidate_prs if pr['number'] != source_pr_number]
        
        print(f"Found {len(candidate_prs)} candidate PRs to analyze")
        
        # Analyze each candidate
        scored_prs = []
        for i, candidate_pr in enumerate(candidate_prs):
            if i % 10 == 0:
                print(f"Analyzing PR {i+1}/{len(candidate_prs)}...", end='\r')
            
            score, reasons = self.score_relevance(source, candidate_pr['number'])
            if score > 0:
                scored_prs.append((candidate_pr['number'], score, reasons))
        
        print(f"\nFound {len(scored_prs)} potentially relevant follow-up PRs")
        
        # Sort by score (highest first)
        scored_prs.sort(key=lambda x: x[1], reverse=True)
        
        return scored_prs[:max_results]
    
    def score_relevance(self, source: Dict, candidate_pr_number: int) -> Tuple[float, Dict]:
        """Score how relevant a candidate PR is as a follow-up."""
        try:
            candidate = self.get_pr_context(candidate_pr_number)
        except Exception as e:
            print(f"Error analyzing PR #{candidate_pr_number}: {e}")
            return 0.0, {}
        
        # Skip if not merged after source
        if not candidate['merged_at'] or candidate['merged_at'] <= source['merged_at']:
            return 0.0, {}
        
        score = 0.0
        reasons = defaultdict(list)
        
        # 1. Same files modified (highest weight)
        common_files = source['files'] & candidate['files']
        if common_files:
            file_score = len(common_files) * 10.0
            score += file_score
            reasons['same_files'].extend(list(common_files)[:5])  # Show up to 5 files
        
        # 2. References same issues
        common_issues = source['issue_refs'] & candidate['issue_refs']
        if common_issues:
            issue_score = len(common_issues) * 8.0
            score += issue_score
            reasons['same_issues'].extend(list(common_issues))
        
        # 3. References the source PR
        candidate_refs = self.extract_issue_refs(candidate['title']) | \
                        self.extract_issue_refs(candidate['body'])
        if source['number'] in candidate_refs:
            score += 15.0
            reasons['references_source_pr'].append(source['number'])
        
        # 4. Similar area/component based on labels
        common_labels = source['labels'] & candidate['labels']
        area_labels = {l for l in common_labels if l.startswith('area/')}
        if area_labels:
            label_score = len(area_labels) * 3.0
            score += label_score
            reasons['same_area_labels'].extend(list(area_labels))
        
        # 5. Keywords indicating follow-up or fix
        followup_keywords = [
            'follow[-_\\s]?up', 'followup', 'follow up',
            'additional fix', 'further fix', 'another fix',
            'regression', 'broken by', 'broke',
            'revert', 'reverts',
            'improve', 'enhancement', 'refinement'
        ]
        
        candidate_text = f"{candidate['title']} {candidate['body']}".lower()
        for keyword in followup_keywords:
            if re.search(keyword, candidate_text):
                score += 2.0
                reasons['followup_keywords'].append(keyword)
                break  # Only count once
        
        # 6. Bug fix for same component
        if 'kind/bugfix' in candidate['labels'] or 'kind/bug' in candidate['labels']:
            if common_files or area_labels:
                score += 5.0
                reasons['bugfix_same_component'].append(True)
        
        # 7. Time proximity (within 30 days gets a bonus)
        days_diff = (candidate['merged_at'] - source['merged_at']).days
        if days_diff <= 30:
            time_score = max(0, 5.0 - (days_diff / 30.0) * 5.0)
            score += time_score
            reasons['time_proximity_days'] = days_diff
        
        return score, dict(reasons)


def format_results(source_pr: int, results: List[Tuple[int, float, Dict]], verbose: bool = False):
    """Format and display results."""
    print(f"\n{'='*80}")
    print(f"Follow-up PRs for #{source_pr}")
    print(f"{'='*80}\n")
    
    if not results:
        print("No follow-up PRs found.")
        return
    
    print(f"Found {len(results)} potential follow-up PRs:\n")
    
    for i, (pr_number, score, reasons) in enumerate(results, 1):
        print(f"{i}. PR #{pr_number} (score: {score:.1f})")
        print(f"   URL: https://github.com/quarkusio/quarkus/pull/{pr_number}")
        
        if verbose:
            if reasons.get('same_files'):
                files = reasons['same_files']
                print(f"   - Modifies same files: {', '.join(files)}")
            
            if reasons.get('same_issues'):
                issues = reasons['same_issues']
                print(f"   - References same issues: {', '.join(f'#{i}' for i in issues)}")
            
            if reasons.get('references_source_pr'):
                print(f"   - Directly references PR #{source_pr}")
            
            if reasons.get('same_area_labels'):
                labels = reasons['same_area_labels']
                print(f"   - Same area labels: {', '.join(labels)}")
            
            if reasons.get('followup_keywords'):
                keywords = reasons['followup_keywords']
                print(f"   - Follow-up keywords: {', '.join(keywords)}")
            
            if reasons.get('bugfix_same_component'):
                print(f"   - Bug fix in same component")
            
            if 'time_proximity_days' in reasons:
                days = reasons['time_proximity_days']
                print(f"   - Merged {days} days after source PR")
        
        print()


def main():
    parser = argparse.ArgumentParser(
        description='Find follow-up PRs that should be backported with a given PR',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Find follow-ups for PR #50799
  python find-followup-prs.py --pr 50799
  
  # With GitHub token for higher API rate limits
  python find-followup-prs.py --pr 50799 --github-token YOUR_TOKEN
  
  # Show detailed reasoning for each follow-up
  python find-followup-prs.py --pr 50799 --verbose
  
  # Output as JSON
  python find-followup-prs.py --pr 50799 --json
        """
    )
    
    parser.add_argument('--pr', type=int, required=True,
                       help='Source PR number to analyze')
    parser.add_argument('--repo', default='quarkusio/quarkus',
                       help='Repository in format owner/repo (default: quarkusio/quarkus)')
    parser.add_argument('--github-token', 
                       help='GitHub API token (or set GITHUB_TOKEN env var)')
    parser.add_argument('--max-results', type=int, default=50,
                       help='Maximum number of follow-up PRs to return (default: 50)')
    parser.add_argument('--verbose', '-v', action='store_true',
                       help='Show detailed reasoning for each follow-up')
    parser.add_argument('--json', action='store_true',
                       help='Output results as JSON')
    
    args = parser.parse_args()
    
    # Initialize API client
    client = GitHubAPIClient(args.github_token)
    
    # Analyze PRs
    analyzer = PRAnalyzer(client, args.repo)
    results = analyzer.find_followups(args.pr, args.max_results)
    
    # Output results
    if args.json:
        output = {
            'source_pr': args.pr,
            'followups': [
                {
                    'pr_number': pr_num,
                    'score': score,
                    'reasons': reasons,
                    'url': f'https://github.com/{args.repo}/pull/{pr_num}'
                }
                for pr_num, score, reasons in results
            ]
        }
        print(json.dumps(output, indent=2))
    else:
        format_results(args.pr, results, args.verbose)


if __name__ == '__main__':
    main()
