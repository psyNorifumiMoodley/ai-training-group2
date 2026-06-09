export type Language = 'JAVA' | 'PYTHON' | 'CSHARP';

export interface TestCaseRequest {
  input: string;
  expectedOutput: string;
  timeoutSeconds: number;
  memoryMb: number;
}

export interface TestCase {
  id: string;
  input: string;
  expectedOutput: string;
  timeoutSeconds: number;
  memoryMb: number;
  ordinal: number;
}
