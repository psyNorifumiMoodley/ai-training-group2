import {
  ChangeDetectionStrategy,
  Component,
  input,
  OnInit,
  output,
  signal,
} from '@angular/core';
import { GroupChildRequest, GroupChildResponse } from '../../../../core/models/question.model';
import { KeywordListComponent } from '../keyword-list/keyword-list.component';
import { ButtonComponent } from '../../../../shared/components/button/button.component';

interface ChildEntry {
  questionText: string;
  keywords: string[];
  marks: number;
}

@Component({
  selector: 'dap-group-children-builder',
  standalone: true,
  imports: [KeywordListComponent, ButtonComponent],
  templateUrl: './group-children-builder.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GroupChildrenBuilderComponent implements OnInit {
  readonly initialChildren = input<GroupChildResponse[]>([]);
  readonly childrenChange = output<GroupChildRequest[]>();

  readonly children = signal<ChildEntry[]>([]);

  ngOnInit(): void {
    const init = this.initialChildren();
    if (init.length > 0) {
      this.children.set(init.map(c => ({
        questionText: c.questionText,
        keywords: [...c.keywords],
        marks: c.marks,
      })));
    }
  }

  addChild(): void {
    this.children.update(list => [...list, { questionText: '', keywords: [], marks: 1 }]);
    this.emit();
  }

  removeChild(index: number): void {
    this.children.update(list => list.filter((_, i) => i !== index));
    this.emit();
  }

  updateChildText(index: number, text: string): void {
    this.children.update(list =>
      list.map((c, i) => i === index ? { ...c, questionText: text } : c)
    );
    this.emit();
  }

  updateChildKeywords(index: number, keywords: string[]): void {
    this.children.update(list =>
      list.map((c, i) => i === index ? { ...c, keywords } : c)
    );
    this.emit();
  }

  updateChildMarks(index: number, marks: number): void {
    this.children.update(list =>
      list.map((c, i) => i === index ? { ...c, marks } : c)
    );
    this.emit();
  }

  private emit(): void {
    this.childrenChange.emit(this.children().map(c => ({
      questionText: c.questionText,
      keywords: c.keywords,
      marks: c.marks,
    })));
  }
}
