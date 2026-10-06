import tseslint from 'typescript-eslint';
export default [{ignores:['dist/**','node_modules/**','e2e/**']},{files:['**/*.{ts,tsx}'],languageOptions:{parser:tseslint.parser},rules:{'no-undef':'off','no-unused-vars':'off','@typescript-eslint/no-unused-vars':'off'}}];
