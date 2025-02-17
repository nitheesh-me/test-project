import os
import sys
import logging
import asyncio
from datetime import datetime
from pathlib import Path
from typing import List, Optional
import javalang
import git
import google.generativeai as genai
from github import Github

# ------ Environment Configuration ------
GH_API_KEY = os.getenv("GH_ACCESS_TOKEN")
REPO_NAME = os.getenv("GH_REPOSITORY")
AI_API_KEY = "AIzaSyCgmBzHg78Ja4W6AyuJ-l4qzvkaiiBFbu4"
FEATURE_BRANCH = "feature-refactor-"

# ------ Code Segmentation Utilities ------
class JavaCodeSegmenter:
    @classmethod
    def segment_code(cls, source: str, chunk_size: int = 6000) -> List[str]:
        """Divide Java source into manageable sections"""
        char_limit = chunk_size * 4
        code_lines = source.split('\n')
        
        try:
            syntax_tree = javalang.parse.parse(source)
            sections = []
            current_section = []
            class_positions = []
            
            for path, node in syntax_tree:
                if isinstance(node, javalang.tree.ClassDeclaration):
                    class_positions.append(node.position.line - 1)
            
            if not class_positions:
                return [source]
            
            for idx, start_line in enumerate(class_positions):
                end_line = class_positions[idx+1] if idx+1 < len(class_positions) else len(code_lines)
                class_code = '\n'.join(code_lines[start_line:end_line])
                
                if len(current_section) + len(class_code) > char_limit and current_section:
                    sections.append('\n'.join(current_section))
                    current_section = [class_code]
                else:
                    current_section.append(class_code)
            
            if current_section:
                sections.append('\n'.join(current_section))
            return sections
            
        except javalang.parser.JavaSyntaxError:
            segments = []
            current_segment = []
            current_length = 0
            
            for line in code_lines:
                if current_length + len(line) > char_limit:
                    if current_segment:
                        segments.append('\n'.join(current_segment))
                    current_segment = [line]
                    current_length = len(line)
                else:
                    current_segment.append(line)
                    current_length += len(line)
            
            if current_segment:
                segments.append('\n'.join(current_segment))
            
            return segments

# ------ AI-Based Code Optimization ------
class AICodeOptimizer:
    def __init__(self, api_key: str):
        genai.configure(api_key=api_key)
        self.llm = genai.GenerativeModel('gemini-pro')

    async def enhance_code(self, original: str) -> str:
        """Process code through AI for quality improvements"""
        code_parts = JavaCodeSegmenter.segment_code(original)
        improved_parts = []
        
        for idx, part in enumerate(code_parts):
            instruction = f"""As an expert Java developer, analyze and improve this code section:
1. Identify architectural issues (SOLID violations, antipatterns)
2. Optimize structure while maintaining functionality
3. Return ONLY the revised code

--- Section {idx+1}/{len(code_parts)} ---
{part}"""
            
            try:
                result = await asyncio.to_thread(self.llm.generate_content, instruction)
                optimized = result.text.strip()
                improved_parts.append(optimized if optimized else part)
            except Exception as err:
                logging.warning(f"AI processing error: {err}")
                improved_parts.append(part)
        
        return self._reconstruct(improved_parts)

    def _reconstruct(self, parts: List[str]) -> str:
        """Combine processed code sections"""
        if not parts:
            return ""
        
        base_section = parts[0].split('\n')
        headers = []
        main_content = []
        
        for line in base_section:
            if line.startswith(('package ', 'import ')):
                headers.append(line)
            else:
                main_content.append(line)
        
        combined = '\n'.join(main_content)
        
        for section in parts[1:]:
            filtered = [ln for ln in section.split('\n') 
                      if not ln.startswith(('package ', 'import '))]
            combined += '\n' + '\n'.join(filtered)
        
        return '\n'.join(headers + [''] + [combined])

# ------ Version Control Integration ------
class GitHubHandler:
    def __init__(self, repository: str, token: str):
        self.repo_name = repository
        self.gh = Github(token)
        self.repo = self.gh.get_repo(repository)
        self.main_branch = self.repo.default_branch

    def download_repository(self, path: str) -> str:
        """Clone repository if missing"""
        if not Path(path).exists():
            logging.info(f"Downloading {self.repo_name}")
            clone_url = f"https://x-access-token:{GH_API_KEY}@github.com/{self.repo_name}.git"
            git.Repo.clone_from(clone_url, path)
        return path

    def make_feature_branch(self, local_path: str) -> str:
        """Create new working branch"""
        repo = git.Repo(local_path)
        repo.git.checkout(self.main_branch)
        new_branch = f"{FEATURE_BRANCH}{datetime.now().strftime('%Y%m%d%H%M')}"
        repo.git.checkout("-b", new_branch)
        return new_branch

    def submit_changes(self, local_path: str, message: str, branch: str):
        """Commit and push modifications"""
        repo = git.Repo(local_path)
        repo.git.add(all=True)
        repo.index.commit(message)
        repo.remote().push(refspec=f"{branch}:{branch}")

    def initiate_pr(self, branch: str, title: str, description: str) -> Optional[str]:
        """Create merge request"""
        try:
            pr = self.repo.create_pull(
                title=title,
                body=description,
                head=branch,
                base=self.main_branch
            )
            return pr.html_url
        except Exception as e:
            logging.error(f"PR creation failed: {e}")
            return None

# ------ Core Workflow ------
async def execute_workflow():
    if not all([GH_API_KEY, REPO_NAME, AI_API_KEY]):
        logging.error("Missing environment configuration")
        sys.exit(1)

    MAX_FILES = 40
    gh_handler = GitHubHandler(REPO_NAME, GH_API_KEY)
    ai_processor = AICodeOptimizer(AI_API_KEY)

    local_path = gh_handler.download_repository("temp_repo")
    feature_branch = gh_handler.make_feature_branch(local_path)

    modified = False
    processed_count = 0
    changed_count = 0

    for root, _, files in os.walk(local_path):
        for fname in [f for f in files if f.endswith(".java")]:
            if processed_count >= MAX_FILES:
                break

            processed_count += 1
            full_path = Path(root) / fname
            logging.info(f"Analyzing {processed_count}/{MAX_FILES}: {fname}")

            with open(full_path, "r+", encoding="utf-8") as file:
                original_code = file.read()
                optimized_code = await ai_processor.enhance_code(original_code)
                
                if optimized_code != original_code:
                    changed_count += 1
                    file.seek(0)
                    file.write(optimized_code)
                    file.truncate()
                    modified = True

        if processed_count >= MAX_FILES:
            break

    if modified:
        commit_msg = "AI-driven code quality improvements"
        gh_handler.submit_changes(local_path, commit_msg, feature_branch)
        
        pr_details = (
            "## Code Quality Enhancements\n\n"
            f"- Processed files: {processed_count}\n"
            f"- Improved files: {changed_count}\n\n"
            "Changes generated through AI analysis of code structure "
            "and architectural patterns."
        )
        
        pr_link = gh_handler.initiate_pr(feature_branch, commit_msg, pr_details)
        if pr_link:
            logging.info(f"Merge request created: {pr_link}")
    else:
        logging.info("No significant changes detected")

# ------ Execution Setup ------
async def main():
    try:
        await execute_workflow()
    except Exception as e:
        logging.error(f"Workflow failure: {e}")
        sys.exit(1)

if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(message)s",
        handlers=[logging.StreamHandler()]
    )
    asyncio.run(main())